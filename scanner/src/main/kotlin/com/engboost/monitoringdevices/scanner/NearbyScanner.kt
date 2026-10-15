package com.engboost.monitoringdevices.scanner

import android.content.Context
import com.engboost.monitoringdevices.scanner.bluetooth.BluetoothScannerApi
import com.engboost.monitoringdevices.scanner.wifi.WifiScannerApi

/**
 * Entry point of the feature: scanners of nearby Wi-Fi networks and Bluetooth devices.
 *
 * The scanners are independent; use only the ones the app needs. Scanning runs only while a
 * scan flow is collected, so it is bound to the scope of the caller, e.g. a visible screen.
 *
 * Pass fake scanners to the primary constructor in tests.
 */
class NearbyScanner(
    val wifi: WifiScannerApi,
    val bluetooth: BluetoothScannerApi
) {
    /** Creates scanners backed by Android APIs with parameters from [config]. */
    constructor(
        context: Context,
        config: NearbyScannerConfig = NearbyScannerConfig()
    ) : this(
        wifi = WifiScannerApi(context, config.wifi),
        bluetooth = BluetoothScannerApi(context, config.bluetooth)
    )
}
