package com.engboost.monitoringdevices.scanner.bluetooth.impl

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothBondState
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceInfo
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceType
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanConfig
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanEvent
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanMode
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val DEVICE_AGE_REFRESH_INTERVAL_MILLIS = 5_000L
private const val DEVICE_RETENTION_MILLIS = 120_000L

class AndroidBluetoothScanner(context: Context) : BluetoothScanner {
    private val appContext = context.applicationContext
    private val bluetoothAdapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    private val diagnostics = BluetoothScanDiagnostics()

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
    override fun scan(config: BluetoothScanConfig): Flow<BluetoothScanEvent> = callbackFlow {
        val adapter = bluetoothAdapter
        val missingPermissions = missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            trySend(BluetoothScanEvent.PermissionRequired(missingPermissions))
            close()
            return@callbackFlow
        }

        diagnostics.unavailableReason(adapter, config.mode)?.let { reason ->
            trySend(BluetoothScanEvent.Unavailable(reason))
            close()
            return@callbackFlow
        }

        if (adapter == null) {
            close()
            return@callbackFlow
        }

        val devicesLock = Any()
        val devices = linkedMapOf<String, BluetoothDeviceInfo>()
        val bleLastSeen = mutableMapOf<String, Long>()

        fun updateDevice(info: BluetoothDeviceInfo, observedViaBle: Boolean) {
            synchronized(devicesLock) {
                devices[info.address] = info
                if (observedViaBle) {
                    bleLastSeen[info.address] = SystemClock.elapsedRealtime()
                }
            }
        }

        fun emitDevices(nowMillis: Long = SystemClock.elapsedRealtime()) {
            val snapshot = synchronized(devicesLock) {
                devices.map { (address, device) ->
                    device.copy(
                        lastSeenAgoMillis = bleLastSeen[address]?.let { lastSeenMillis ->
                            (nowMillis - lastSeenMillis).coerceAtLeast(0L)
                        }
                    )
                }
            }
            trySend(BluetoothScanEvent.Devices(snapshot))
        }

        fun readDeviceInfo(
            device: BluetoothDevice,
            rssiDbm: Int?
        ): BluetoothDeviceInfo? {
            return try {
                device.toBluetoothDeviceInfo(rssiDbm)
            } catch (error: SecurityException) {
                val missingPermissions = missingPermissions()
                if (missingPermissions.isNotEmpty()) {
                    trySend(BluetoothScanEvent.PermissionRequired(missingPermissions))
                } else {
                    trySend(BluetoothScanEvent.Error("Bluetooth permission check failed", error))
                }
                close()
                null
            } catch (error: Exception) {
                trySend(BluetoothScanEvent.Error("Failed to read Bluetooth device", error))
                close()
                null
            }
        }

        trySend(BluetoothScanEvent.Scanning)
        val agingJob = if (config.mode != BluetoothScanMode.CLASSIC) {
            launch {
                while (true) {
                    delay(DEVICE_AGE_REFRESH_INTERVAL_MILLIS)
                    val nowMillis = SystemClock.elapsedRealtime()
                    val shouldEmit = synchronized(devicesLock) {
                        val hadBleDevices = bleLastSeen.isNotEmpty()
                        val expiredAddresses = bleLastSeen
                            .filterValues { lastSeenMillis ->
                                nowMillis - lastSeenMillis >= DEVICE_RETENTION_MILLIS
                            }
                            .keys
                        expiredAddresses.forEach { address ->
                            bleLastSeen.remove(address)
                            devices.remove(address)
                        }
                        hadBleDevices
                    }
                    if (shouldEmit) emitDevices(nowMillis)
                }
            }
        } else {
            null
        }

        val classicReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != BluetoothDevice.ACTION_FOUND) return

                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    .takeUnless { it == Short.MIN_VALUE }
                    ?.toInt()

                if (device != null) {
                    val info = readDeviceInfo(device, rssi) ?: return
                    updateDevice(info, observedViaBle = false)
                    emitDevices()
                }
            }
        }

        val bleCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val info = readDeviceInfo(result.device, result.rssi) ?: return
                updateDevice(info, observedViaBle = true)
                emitDevices()
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (result in results) {
                    val info = readDeviceInfo(result.device, result.rssi) ?: return
                    updateDevice(info, observedViaBle = true)
                }
                emitDevices()
            }

            override fun onScanFailed(errorCode: Int) {
                trySend(BluetoothScanEvent.Error("Bluetooth LE scan failed: $errorCode"))
                close()
            }
        }

        var startupFailed = false

        if (config.mode == BluetoothScanMode.CLASSIC || config.mode == BluetoothScanMode.ALL) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(classicReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                appContext.registerReceiver(classicReceiver, filter)
            }
            runCatching { adapter.startDiscovery() }
                .onSuccess { isStarted ->
                    if (!isStarted) {
                        trySend(BluetoothScanEvent.Unavailable(diagnostics.classicDiscoveryRejectedReason()))
                        startupFailed = true
                        close()
                    }
                }
                .onFailure { error ->
                    trySend(BluetoothScanEvent.Error("Failed to start classic Bluetooth discovery", error))
                    startupFailed = true
                    close()
                }
        }

        if (!startupFailed && (config.mode == BluetoothScanMode.BLE || config.mode == BluetoothScanMode.ALL)) {
            val bleScanner = adapter.bluetoothLeScanner
            if (bleScanner == null) {
                trySend(BluetoothScanEvent.Unavailable("Bluetooth LE scanner is not available"))
                close()
            } else {
                runCatching { bleScanner.startScan(bleCallback) }
                    .onFailure { error ->
                        trySend(BluetoothScanEvent.Error("Failed to start BLE scan", error))
                        close()
                    }
            }
        }

        awaitClose {
            agingJob?.cancel()
            if (config.mode == BluetoothScanMode.CLASSIC || config.mode == BluetoothScanMode.ALL) {
                runCatching { adapter.cancelDiscovery() }
                runCatching { appContext.unregisterReceiver(classicReceiver) }
            }
            if (config.mode == BluetoothScanMode.BLE || config.mode == BluetoothScanMode.ALL) {
                runCatching { adapter.bluetoothLeScanner?.stopScan(bleCallback) }
            }
        }
    }

    private fun missingPermissions(): Set<String> {
        return requiredPermissions
            .filter { permission ->
                appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
            }
            .toSet()
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toBluetoothDeviceInfo(rssiDbm: Int?): BluetoothDeviceInfo {
        return BluetoothDeviceInfo(
            name = runCatching { name }.getOrNull(),
            address = runCatching { address }.getOrDefault("unknown"),
            rssiDbm = rssiDbm,
            type = type.toBluetoothDeviceType(),
            bondState = bondState.toBluetoothBondState(),
            lastSeenAgoMillis = null
        )
    }

    private fun Int.toBluetoothDeviceType(): BluetoothDeviceType {
        return when (this) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> BluetoothDeviceType.CLASSIC
            BluetoothDevice.DEVICE_TYPE_LE -> BluetoothDeviceType.BLE
            BluetoothDevice.DEVICE_TYPE_DUAL -> BluetoothDeviceType.DUAL
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
