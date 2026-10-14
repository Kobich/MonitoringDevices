package com.engboost.monitoringdevices.scanner

import com.engboost.monitoringdevices.scanner.bluetooth.BluetoothScanConfig
import com.engboost.monitoringdevices.scanner.wifi.WifiScanConfig

/**
 * Configuration of [NearbyScanner].
 *
 * @property wifi parameters of Wi-Fi scanning.
 * @property bluetooth parameters of Bluetooth scanning.
 */
data class NearbyScannerConfig(
    val wifi: WifiScanConfig = WifiScanConfig(),
    val bluetooth: BluetoothScanConfig = BluetoothScanConfig()
)
