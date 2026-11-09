package com.engboost.monitoringdevices.scanner.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager

internal class BluetoothScanAvailability(
    private val context: Context,
    private val adapter: BluetoothAdapter?
) {
    val requiredPermissions: Set<String> = setOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    fun missingPermissions(): Set<String> {
        return requiredPermissions.filterTo(linkedSetOf()) { permission ->
            context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun unavailableReason(): BluetoothUnavailableReason? {
        return when {
            adapter == null -> BluetoothUnavailableReason.ADAPTER_UNAVAILABLE
            !adapter.isEnabled -> BluetoothUnavailableReason.BLUETOOTH_DISABLED
            adapter.bluetoothLeScanner == null -> BluetoothUnavailableReason.LE_SCANNER_UNAVAILABLE
            else -> null
        }
    }
}
