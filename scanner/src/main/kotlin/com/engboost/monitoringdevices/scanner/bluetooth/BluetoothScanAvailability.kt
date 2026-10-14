package com.engboost.monitoringdevices.scanner.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Checks permissions and adapter state required for Bluetooth LE scanning. */
internal class BluetoothScanAvailability(
    private val context: Context,
    private val adapter: BluetoothAdapter?
) {
    val requiredPermissions: Set<String> = buildSet {
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

    fun missingPermissions(): Set<String> {
        return requiredPermissions.filterTo(linkedSetOf()) { permission ->
            context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /** Returns why scanning is impossible right now, or `null` if it can start. */
    fun unavailableReason(): BluetoothUnavailableReason? {
        return when {
            adapter == null -> BluetoothUnavailableReason.ADAPTER_UNAVAILABLE
            !adapter.isEnabled -> BluetoothUnavailableReason.BLUETOOTH_DISABLED
            adapter.bluetoothLeScanner == null -> BluetoothUnavailableReason.LE_SCANNER_UNAVAILABLE
            else -> null
        }
    }
}
