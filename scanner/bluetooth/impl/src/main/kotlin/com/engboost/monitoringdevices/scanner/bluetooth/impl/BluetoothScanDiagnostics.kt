package com.engboost.monitoringdevices.scanner.bluetooth.impl

import android.bluetooth.BluetoothAdapter
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanMode

internal class BluetoothScanDiagnostics {
    fun unavailableReason(
        adapter: BluetoothAdapter?,
        mode: BluetoothScanMode
    ): String? {
        return when {
            adapter == null -> "Bluetooth adapter is not available"
            !adapter.isEnabled -> "Bluetooth is disabled"
            mode.requiresBle() && adapter.bluetoothLeScanner == null -> "Bluetooth LE scanner is not available"
            else -> null
        }
    }

    fun classicDiscoveryRejectedReason(): String {
        return "Classic Bluetooth discovery was not started by Android."
    }

    private fun BluetoothScanMode.requiresBle(): Boolean {
        return this == BluetoothScanMode.BLE || this == BluetoothScanMode.ALL
    }
}
