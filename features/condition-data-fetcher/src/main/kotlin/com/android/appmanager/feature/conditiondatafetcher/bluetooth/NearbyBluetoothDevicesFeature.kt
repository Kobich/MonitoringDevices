package com.android.appmanager.feature.conditiondatafetcher.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import com.android.appmanager.feature.conditiondatafetcher.BtFoundCondition
import com.android.appmanager.feature.conditiondatafetcher.BtFoundData
import com.android.appmanager.feature.conditiondatafetcher.ConditionFeature
import com.android.appmanager.feature.conditiondatafetcher.ConditionResult
import com.android.appmanager.feature.conditiondatafetcher.DataFeature
import com.android.appmanager.feature.conditiondatafetcher.SourceId
import com.android.appmanager.feature.conditiondatafetcher.UnknownReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Every call runs its own BLE scan of up to [SCAN_WINDOW], at most once per 30 seconds.
 *
 * [getData] scans without a filter, which gets no results while the screen is off.
 * [isMeetsCondition] filters by name and stops at the first match, so it works with the screen off.
 * Many devices do not advertise a name and cannot be found this way.
 */
@SuppressLint("MissingPermission")
class NearbyBluetoothDevicesFeature(context: Context) :
    DataFeature<BtFoundData>,
    ConditionFeature<BtFoundCondition> {

    override fun getId() = SourceId.BT_FOUND

    private val appContext = context.applicationContext
    private val bluetoothAdapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter

    override suspend fun getData(): BtFoundData? {
        val devices = LinkedHashMap<String, NearbyBluetoothDevice>()
        val failure = scan(
            filters = emptyList(),
            stopWhen = { device ->
                devices[device.address] = device
                false // collect everything for the whole window
            }
        )
        return if (failure == null) BtFoundData(devices.values.toList()) else null
    }

    override suspend fun isMeetsCondition(input: BtFoundCondition): ConditionResult {
        val names = input.names.toSet()
        if (names.isEmpty()) return ConditionResult.False

        val filters = names.map { name -> ScanFilter.Builder().setDeviceName(name).build() }
        var isFound = false
        val failure = scan(
            filters = filters,
            stopWhen = { device ->
                isFound = device.name?.let { it in names } == true
                isFound // stop as soon as a wanted device is found
            }
        )
        return if (failure != null) ConditionResult.Unknown(failure) else ConditionResult.of(isFound)
    }

    /** Returns null when the scan ran, otherwise why it could not. */
    private suspend fun scan(
        filters: List<ScanFilter>,
        stopWhen: (NearbyBluetoothDevice) -> Boolean
    ): UnknownReason? {
        // BLUETOOTH_SCAN has no neverForLocation flag: without location permission Android
        // silently drops every result.
        val permissions = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.any { appContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            return UnknownReason.PERMISSION_DENIED
        }
        val adapter = bluetoothAdapter ?: return UnknownReason.SOURCE_UNAVAILABLE
        if (!adapter.isEnabled) return UnknownReason.SOURCE_DISABLED
        val leScanner = adapter.bluetoothLeScanner ?: return UnknownReason.SOURCE_UNAVAILABLE
        if (!tryReserveScanStart()) return UnknownReason.TOO_FREQUENT

        val results = Channel<NearbyBluetoothDevice>(Channel.UNLIMITED)
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                results.trySend(result.toNearbyDevice())
            }

            override fun onScanFailed(errorCode: Int) {
                results.close(ScanFailedException(errorCode))
            }
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            leScanner.startScan(filters, settings, callback)
        } catch (error: SecurityException) {
            return UnknownReason.PERMISSION_DENIED
        }

        return try {
            withTimeoutOrNull(SCAN_WINDOW) {
                for (device in results) {
                    if (stopWhen(device)) break
                }
            }
            null
        } catch (error: ScanFailedException) {
            UnknownReason.READ_FAILED
        } finally {
            runCatching { leScanner.stopScan(callback) }
        }
    }

    private fun ScanResult.toNearbyDevice() = NearbyBluetoothDevice(
        // The scan filter matches the advertised name, which also needs no permission.
        name = scanRecord?.deviceName?.takeIf { it.isNotBlank() }
            ?: runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() },
        address = device.address,
        rssiDbm = rssi
    )

    private class ScanFailedException(errorCode: Int) : Exception("Bluetooth LE scan failed: $errorCode")

    private companion object {
        val SCAN_WINDOW = 10.seconds

        // Android allows 5 BLE scan starts per 30 s per app. The scanner screen takes up to 4,
        // this feature 1. Keep both limits in sync. Static because the limit is per app.
        const val MIN_SCAN_START_INTERVAL_MILLIS = 30_000L
        var lastScanStartMillis: Long? = null

        @Synchronized
        fun tryReserveScanStart(): Boolean {
            val nowMillis = SystemClock.elapsedRealtime()
            val lastStart = lastScanStartMillis
            if (lastStart != null && nowMillis - lastStart < MIN_SCAN_START_INTERVAL_MILLIS) return false

            lastScanStartMillis = nowMillis
            return true
        }
    }
}
