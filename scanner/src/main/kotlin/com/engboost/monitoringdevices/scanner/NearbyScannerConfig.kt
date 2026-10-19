package com.engboost.monitoringdevices.scanner

import com.engboost.monitoringdevices.scanner.bluetooth.BluetoothScanConfig
import com.engboost.monitoringdevices.scanner.wifi.WifiScanConfig

data class NearbyScannerConfig(
    val wifi: WifiScanConfig = WifiScanConfig(),
    val bluetooth: BluetoothScanConfig = BluetoothScanConfig()
)
