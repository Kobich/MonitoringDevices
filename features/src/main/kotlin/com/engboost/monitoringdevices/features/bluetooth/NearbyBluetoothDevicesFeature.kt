package com.engboost.monitoringdevices.features.bluetooth

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
import com.engboost.monitoringdevices.features.ConditionFeature
import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.DataFeature
import com.engboost.monitoringdevices.features.UnknownReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Nearby Bluetooth LE devices. Each call runs a short scan of up to [SCAN_WINDOW].
 *
 * [getData] scans without a filter, so it finds nothing while the screen is off.
 * Returns null when the scan could not run.
 * [isMeetsCondition] scans with a filter by the given names and stops as soon as one is found,
 * so it also works while the screen is off. Matches only the name the device advertises.
 *
 * Scans start at most once per [MIN_SCAN_START_INTERVAL_MILLIS]; a call made earlier
 * gets null or [UnknownReason.TOO_FREQUENT].
 *
 * Unknown reasons: [UnknownReason.PERMISSION_DENIED], [UnknownReason.SOURCE_UNAVAILABLE],
 * [UnknownReason.SOURCE_DISABLED], [UnknownReason.TOO_FREQUENT], [UnknownReason.READ_FAILED].
 */
@SuppressLint("MissingPermission")
class NearbyBluetoothDevicesFeature(context: Context) :
    DataFeature<List<NearbyBluetoothDevice>>,
    ConditionFeature<List<String>> {

    private val appContext = context.applicationContext
    private val bluetoothAdapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter

    override suspend fun getData(): List<NearbyBluetoothDevice>? {
        val devices = LinkedHashMap<String, NearbyBluetoothDevice>()
        val failure = scan(
            filters = emptyList(),
            stopWhen = { device ->
                devices[device.address] = device
                false // collect everything for the whole window
            }
        )
        return if (failure == null) devices.values.toList() else null
    }

    override suspend fun isMeetsCondition(input: List<String>): ConditionResult {
        val names = input.toSet()
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

    /**
     * Calls [stopWhen] for every found device and scans until it returns true
     * or [SCAN_WINDOW] passes.
     * Returns null when the scan ran, or the reason it could not run or failed.
     */
    private suspend fun scan(
        filters: List<ScanFilter>,
        stopWhen: (NearbyBluetoothDevice) -> Boolean
    ): UnknownReason? {
        // BLUETOOTH_SCAN is declared without neverForLocation, so without location permission
        // the scan runs but Android silently drops every result.
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
        // The advertised name needs no permission and is what the scan filter matches.
        name = scanRecord?.deviceName?.takeIf { it.isNotBlank() }
            ?: runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() },
        address = device.address,
        rssiDbm = rssi
    )

    private class ScanFailedException(errorCode: Int) : Exception("Bluetooth LE scan failed: $errorCode")

    private companion object {
        val SCAN_WINDOW = 10.seconds

        // Android allows 5 BLE scan starts per 30 seconds per app. The scanner screen takes up
        // to 4 of them, this feature takes 1. Shared by all instances because the limit is per app.
        const val MIN_SCAN_START_INTERVAL_MILLIS = 30_000L
        var lastScanStartMillis: Long? = null

        /** Reserves a scan start, or returns false if the previous one was too recent. */
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
