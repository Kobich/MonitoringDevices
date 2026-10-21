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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Nearby Bluetooth LE devices. Each call runs a short scan of up to [SCAN_WINDOW_MILLIS].
 *
 * [getData] scans without a filter, so it finds nothing while the screen is off.
 * Returns null when the scan could not run.
 * [isMeetsCondition] scans with a filter by the given names and stops as soon as one is found,
 * so it also works while the screen is off. Matches only the name the device advertises.
 *
 * Scans start at most once per [MIN_SCAN_START_INTERVAL_MILLIS]; a call made earlier
 * gets null or [ConditionResult.UNKNOWN].
 */
@SuppressLint("MissingPermission")
class NearbyBluetoothDevicesFeature(context: Context) :
    DataFeature<List<NearbyBluetoothDevice>>,
    ConditionFeature<List<String>> {

    private val appContext = context.applicationContext
    private val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter

    override suspend fun getData(): List<NearbyBluetoothDevice>? {
        val devices = LinkedHashMap<String, NearbyBluetoothDevice>()
        val isScanned = scan(filters = emptyList()) { device ->
            devices[device.address] = device
            false
        }
        return if (isScanned) devices.values.toList() else null
    }

    override suspend fun isMeetsCondition(input: List<String>): ConditionResult {
        val names = input.toSet()
        if (names.isEmpty()) return ConditionResult.FALSE

        val filters = names.map { name -> ScanFilter.Builder().setDeviceName(name).build() }
        var isFound = false
        val isScanned = scan(filters) { device ->
            isFound = device.name?.let { it in names } == true
            isFound
        }
        return when {
            !isScanned -> ConditionResult.UNKNOWN
            isFound -> ConditionResult.TRUE
            else -> ConditionResult.FALSE
        }
    }

    /**
     * Scans until [onDevice] returns true or [SCAN_WINDOW_MILLIS] passes.
     * Returns false when the scan could not run or failed.
     */
    private suspend fun scan(
        filters: List<ScanFilter>,
        onDevice: (NearbyBluetoothDevice) -> Boolean
    ): Boolean {
        if (appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val leScanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return false
        if (!tryReserveScanStart()) return false

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
            return false
        }

        return try {
            withTimeoutOrNull(SCAN_WINDOW_MILLIS) {
                for (device in results) {
                    if (onDevice(device)) break
                }
            }
            true
        } catch (error: ScanFailedException) {
            false
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
        const val SCAN_WINDOW_MILLIS = 10_000L

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
