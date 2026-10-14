package com.engboost.monitoringdevices.monitoring.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Scans nearby devices with an unfiltered Bluetooth LE scan.
 *
 * Bluetooth Classic discovery is not used: an LE scan also finds dual-mode devices.
 * Android suspends unfiltered scans while the screen is off.
 */
internal class BluetoothScanner(
    context: Context,
    private val config: BluetoothMonitoringConfig
) {
    private val adapter = context.applicationContext
        .getSystemService(BluetoothManager::class.java)
        ?.adapter
    private val availability = BluetoothScanAvailability(context.applicationContext, adapter)

    val requiredPermissions: Set<String>
        get() = availability.requiredPermissions

    fun missingPermissions(): Set<String> = availability.missingPermissions()

    /** Returns a cold flow that scans until it is cancelled or a terminal event is sent. */
    @SuppressLint("MissingPermission")
    fun scan(): Flow<BluetoothScanEvent> = callbackFlow {
        val missingPermissions = availability.missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            trySend(BluetoothScanEvent.PermissionRequired(missingPermissions))
            close()
            return@callbackFlow
        }

        val leScanner = adapter?.bluetoothLeScanner
        val unavailableReason = availability.unavailableReason()
        if (unavailableReason != null || leScanner == null) {
            val reason = unavailableReason ?: BluetoothUnavailableReason.LE_SCANNER_UNAVAILABLE
            trySend(BluetoothScanEvent.Unavailable(reason))
            close()
            return@callbackFlow
        }

        val registry = BluetoothDeviceRegistry(config)

        fun sendSnapshot(force: Boolean) {
            registry.snapshot(SystemClock.elapsedRealtime(), force)?.let { devices ->
                trySend(BluetoothScanEvent.Devices(devices))
            }
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val advertisement = readAdvertisement(result) ?: return
                registry.onAdvertisement(advertisement, SystemClock.elapsedRealtime())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (result in results) {
                    val advertisement = readAdvertisement(result) ?: return
                    registry.onAdvertisement(advertisement, SystemClock.elapsedRealtime())
                }
            }

            override fun onScanFailed(errorCode: Int) {
                trySend(BluetoothScanEvent.Error("Bluetooth LE scan failed: $errorCode"))
                close()
            }
        }

        trySend(BluetoothScanEvent.Scanning)
        val startError = runCatching { leScanner.startScan(callback) }.exceptionOrNull()
        if (startError != null) {
            trySend(startError.toScanFailureEvent("Failed to start Bluetooth LE scan"))
            close()
            return@callbackFlow
        }

        val snapshotJob = launch {
            while (isActive) {
                delay(config.snapshotIntervalMillis)
                sendSnapshot(force = false)
            }
        }
        val agingJob = launch {
            while (isActive) {
                delay(config.ageRefreshIntervalMillis)
                if (registry.removeExpired(SystemClock.elapsedRealtime())) sendSnapshot(force = true)
            }
        }

        awaitClose {
            snapshotJob.cancel()
            agingJob.cancel()
            runCatching { leScanner.stopScan(callback) }
        }
    }

    /** Reads [result]; on failure sends a terminal event, closes the flow and returns `null`. */
    private fun ProducerScope<BluetoothScanEvent>.readAdvertisement(result: ScanResult): BluetoothAdvertisement? {
        return try {
            result.toAdvertisement()
        } catch (error: Exception) {
            trySend(error.toScanFailureEvent("Failed to read Bluetooth device"))
            close()
            null
        }
    }

    private fun Throwable.toScanFailureEvent(action: String): BluetoothScanEvent {
        val missingPermissions = availability.missingPermissions()
        if (this is SecurityException && missingPermissions.isNotEmpty()) {
            return BluetoothScanEvent.PermissionRequired(missingPermissions)
        }
        return BluetoothScanEvent.Error(action, this)
    }

    @SuppressLint("MissingPermission")
    private fun ScanResult.toAdvertisement(): BluetoothAdvertisement {
        return BluetoothAdvertisement(
            // The cached name needs BLUETOOTH_CONNECT; the advertised name does not.
            name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: scanRecord?.deviceName?.takeIf { it.isNotBlank() },
            address = device.address,
            rssiDbm = rssi,
            type = device.type.toBluetoothDeviceType(),
            bondState = device.bondState.toBluetoothBondState()
        )
    }

    private fun Int.toBluetoothDeviceType(): BluetoothDeviceType {
        return when (this) {
            BluetoothDevice.DEVICE_TYPE_LE -> BluetoothDeviceType.LE
            BluetoothDevice.DEVICE_TYPE_DUAL -> BluetoothDeviceType.DUAL
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> BluetoothDeviceType.CLASSIC
            else -> BluetoothDeviceType.UNKNOWN
        }
    }

    private fun Int.toBluetoothBondState(): BluetoothBondState {
        return when (this) {
            BluetoothDevice.BOND_NONE -> BluetoothBondState.NONE
            BluetoothDevice.BOND_BONDING -> BluetoothBondState.BONDING
            BluetoothDevice.BOND_BONDED -> BluetoothBondState.BONDED
            else -> BluetoothBondState.UNKNOWN
        }
    }
}
