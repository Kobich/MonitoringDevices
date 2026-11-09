package com.android.appmanager.feature.nearbyscanner

import com.android.appmanager.feature.nearbyscanner.bluetooth.BluetoothScanConfig
import com.android.appmanager.feature.nearbyscanner.wifi.WifiScanConfig

data class NearbyScannerConfig(
    val wifi: WifiScanConfig = WifiScanConfig(),
    val bluetooth: BluetoothScanConfig = BluetoothScanConfig()
)
