package com.engboost.monitoringdevices.monitoring.internal.ble

import android.bluetooth.BluetoothAdapter
import com.engboost.monitoringdevices.monitoring.ble.BleUnavailableReason

internal class BleScanDiagnostics {
    fun unavailableReason(adapter: BluetoothAdapter?): BleUnavailableReason? {
        return when {
            adapter == null -> BleUnavailableReason.ADAPTER_UNAVAILABLE
            !adapter.isEnabled -> BleUnavailableReason.BLUETOOTH_DISABLED
            adapter.bluetoothLeScanner == null -> BleUnavailableReason.LE_SCANNER_UNAVAILABLE
            else -> null
        }
    }
}
