package com.engboost.monitoringdevices.monitoring.internal.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import com.engboost.monitoringdevices.monitoring.BleMonitoringConfig
import com.engboost.monitoringdevices.monitoring.ble.BleBondState
import com.engboost.monitoringdevices.monitoring.ble.BleDeviceType
import com.engboost.monitoringdevices.monitoring.ble.BleScanEvent
import com.engboost.monitoringdevices.monitoring.ble.BleUnavailableReason
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class AndroidBleScanner(
    context: Context,
    private val config: BleMonitoringConfig
) : BleScanner {
    private val appContext = context.applicationContext
    private val bluetoothAdapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    private val diagnostics = BleScanDiagnostics()

    override val requiredPermissions: Set<String> = buildSet {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    override fun scan(): Flow<BleScanEvent> = callbackFlow {
        val missingPermissions = missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            trySend(BleScanEvent.PermissionRequired(missingPermissions))
            close()
            return@callbackFlow
        }

        diagnostics.unavailableReason(bluetoothAdapter)?.let { reason ->
            trySend(BleScanEvent.Unavailable(reason))
            close()
            return@callbackFlow
        }

        val leScanner = bluetoothAdapter?.bluetoothLeScanner
        if (leScanner == null) {
            trySend(BleScanEvent.Unavailable(BleUnavailableReason.LE_SCANNER_UNAVAILABLE))
            close()
            return@callbackFlow
        }

        val registry = BleDeviceRegistry(config)

        fun emitSnapshot(force: Boolean) {
            registry.snapshot(SystemClock.elapsedRealtime(), force)?.let { devices ->
                trySend(BleScanEvent.Devices(devices))
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
                trySend(BleScanEvent.Error("Bluetooth LE scan failed: $errorCode"))
                close()
            }
        }

        trySend(BleScanEvent.Scanning)
        val startError = runCatching { leScanner.startScan(callback) }.exceptionOrNull()
        if (startError != null) {
            trySend(startError.toScanFailureEvent("Failed to start BLE scan"))
            close()
            return@callbackFlow
        }

        val snapshotJob = launch {
            while (isActive) {
                delay(config.snapshotIntervalMillis)
                emitSnapshot(force = false)
            }
        }
        val agingJob = launch {
            while (isActive) {
                delay(config.ageRefreshIntervalMillis)
                if (registry.removeExpired(SystemClock.elapsedRealtime())) emitSnapshot(force = true)
            }
        }

        awaitClose {
            snapshotJob.cancel()
            agingJob.cancel()
            runCatching { leScanner.stopScan(callback) }
        }
    }

    private fun ProducerScope<BleScanEvent>.readAdvertisement(result: ScanResult): BleAdvertisement? {
        return try {
            result.toAdvertisement()
        } catch (error: Exception) {
            trySend(error.toScanFailureEvent("Failed to read BLE device"))
            close()
            null
        }
    }

    private fun Throwable.toScanFailureEvent(action: String): BleScanEvent {
        val missingPermissions = missingPermissions()
        if (this is SecurityException && missingPermissions.isNotEmpty()) {
            return BleScanEvent.PermissionRequired(missingPermissions)
        }
        return BleScanEvent.Error(action, this)
    }

    private fun missingPermissions(): Set<String> {
        return requiredPermissions
            .filter { permission ->
                appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
            }
            .toSet()
    }

    @SuppressLint("MissingPermission")
    private fun ScanResult.toAdvertisement(): BleAdvertisement {
        return BleAdvertisement(
            name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: scanRecord?.deviceName?.takeIf { it.isNotBlank() },
            address = device.address,
            rssiDbm = rssi,
            type = device.type.toBleDeviceType(),
            bondState = device.bondState.toBleBondState()
        )
    }

    private fun Int.toBleDeviceType(): BleDeviceType {
        return when (this) {
            BluetoothDevice.DEVICE_TYPE_LE -> BleDeviceType.LE
            BluetoothDevice.DEVICE_TYPE_DUAL -> BleDeviceType.DUAL
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> BleDeviceType.CLASSIC
            else -> BleDeviceType.UNKNOWN
        }
    }

    private fun Int.toBleBondState(): BleBondState {
        return when (this) {
            BluetoothDevice.BOND_NONE -> BleBondState.NONE
            BluetoothDevice.BOND_BONDING -> BleBondState.BONDING
            BluetoothDevice.BOND_BONDED -> BleBondState.BONDED
            else -> BleBondState.UNKNOWN
        }
    }
}
