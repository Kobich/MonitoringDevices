package com.engboost.monitoringdevices.scanner.wifi.impl

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

internal class WifiScanThrottlingStatusReader(
    private val appContext: Context
) {
    fun readThrottleStatus(): Boolean {
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return wifi.isScanThrottleEnabled
        return try {
            Settings.Global.getInt(appContext.contentResolver, "wifi_scan_throttle_enabled") == 1
        } catch (_: Exception) { true }
    }
}
