package com.android.appmanager.feature.scanner

import android.content.Context
import com.android.appmanager.feature.scanner.bluetooth.BluetoothScannerApi
import com.android.appmanager.feature.scanner.wifi.WifiScannerApi

/**
 * Entry point of the feature. The scanners are independent; use only the ones the app needs.
 *
 * The primary constructor accepts fake scanners for tests.
 */
class NearbyScanner(
    val wifi: WifiScannerApi,
    val bluetooth: BluetoothScannerApi
) {
    constructor(
        context: Context,
        config: NearbyScannerConfig = NearbyScannerConfig()
    ) : this(
        wifi = WifiScannerApi(context, config.wifi),
        bluetooth = BluetoothScannerApi(context, config.bluetooth)
    )
}
