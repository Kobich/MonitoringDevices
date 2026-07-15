package com.engboost.monitoringdevices.scanner.wifi.impl

import android.content.Context
import android.provider.Settings
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanThrottlingStatus

internal class WifiScanThrottlingStatusReader(
    private val appContext: Context
) {
    fun readStatus(): WifiScanThrottlingStatus {
        val isEnabled = runCatching {
            Settings.Global.getInt(
                appContext.contentResolver,
                WIFI_SCAN_THROTTLE_ENABLED,
                WIFI_SCAN_THROTTLE_ENABLED_VALUE
            ) == WIFI_SCAN_THROTTLE_ENABLED_VALUE
        }.getOrNull()

        return WifiScanThrottlingStatus(isEnabled = isEnabled)
    }

    private companion object {
        const val WIFI_SCAN_THROTTLE_ENABLED = "wifi_scan_throttle_enabled"
        const val WIFI_SCAN_THROTTLE_ENABLED_VALUE = 1
    }
}
