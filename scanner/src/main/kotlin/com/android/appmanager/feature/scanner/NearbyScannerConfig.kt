package com.android.appmanager.feature.scanner

import com.android.appmanager.feature.scanner.bluetooth.BluetoothScanConfig
import com.android.appmanager.feature.scanner.wifi.WifiScanConfig

data class NearbyScannerConfig(
    val wifi: WifiScanConfig = WifiScanConfig(),
    val bluetooth: BluetoothScanConfig = BluetoothScanConfig()
)
