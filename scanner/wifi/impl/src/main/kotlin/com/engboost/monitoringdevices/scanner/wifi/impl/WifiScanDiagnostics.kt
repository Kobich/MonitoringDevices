package com.engboost.monitoringdevices.scanner.wifi.impl

import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build

internal class WifiScanDiagnostics(
    private val appContext: Context,
    private val wifiManager: WifiManager
) {
    fun unavailableReason(): String? {
        return when {
            !wifiManager.isWifiEnabled -> "Wi-Fi is disabled"
            !isLocationEnabled() -> "Location Services are disabled"
            else -> null
        }
    }

    fun activeScanRejectedReason(hasCachedResults: Boolean): String {
        return if (hasCachedResults) {
            "Active Wi-Fi scan was throttled by Android. Showing cached results."
        } else {
            "Wi-Fi scan was not started by Android. Check Wi-Fi, Location Services and scan throttling."
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = appContext.getSystemService(LocationManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }
    }
}
