package com.engboost.monitoringdevices.scanner.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.SystemClock
import android.util.SparseArray
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Android suspends this unfiltered scan while the screen is off. */
internal class BluetoothScannerImpl(
    context: Context,
    private val config: BluetoothScanConfig
) : BluetoothScannerApi {
    private val adapter = context.applicationContext
        .getSystemService(BluetoothManager::class.java)
        ?.adapter
    private val availability = BluetoothScanAvailability(context.applicationContext, adapter)

    override val requiredPermissions: Set<String>
        get() = availability.requiredPermissions

    override fun missingPermissions(): Set<String> = availability.missingPermissions()

    @SuppressLint("MissingPermission")
    override fun scan(): Flow<BluetoothScanEvent> = callbackFlow {
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
                try {
                    registry.onAdvertisement(result.toAdvertisement(), SystemClock.elapsedRealtime())
                } catch (error: SecurityException) {
                    trySend(error.toScanFailureEvent("Failed to read Bluetooth device"))
                    close()
                }
            }

            override fun onScanFailed(errorCode: Int) {
                trySend(BluetoothScanEvent.Error("Bluetooth LE scan failed: $errorCode"))
                close()
            }
        }

        trySend(BluetoothScanEvent.Scanning)
        startLimiter.awaitStart()
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

    private companion object {
        // Android allows 5 BLE scan starts per 30 seconds per app. 8 seconds gives this scanner
        // at most 4 starts and leaves at least 1 for the monitoring scan.
        // Shared by all instances because the system limit is per app.
        val startLimiter = ScanStartLimiter(minIntervalMillis = 8_000L)
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
            bondState = device.bondState.toBluetoothBondState(),
            manufacturerData = scanRecord?.manufacturerSpecificData?.toHexMap().orEmpty(),
            serviceUuids = scanRecord?.serviceUuids.orEmpty().map { parcelUuid -> parcelUuid.uuid }
        )
    }

    private fun SparseArray<ByteArray>.toHexMap(): Map<Int, String> {
        return (0 until size()).associate { index ->
            keyAt(index) to valueAt(index).joinToString(separator = "") { byte -> "%02X".format(byte) }
        }
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
