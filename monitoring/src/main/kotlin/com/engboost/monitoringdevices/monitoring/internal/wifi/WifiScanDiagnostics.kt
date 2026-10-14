package com.engboost.monitoringdevices.monitoring.internal.wifi

import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import com.engboost.monitoringdevices.monitoring.wifi.WifiUnavailableReason

internal class WifiScanDiagnostics(
    private val appContext: Context,
    private val wifiManager: WifiManager
) {
    fun unavailableReason(): WifiUnavailableReason? {
        return when {
            !wifiManager.isWifiEnabled -> WifiUnavailableReason.WIFI_DISABLED
            !isLocationEnabled() -> WifiUnavailableReason.LOCATION_DISABLED
            else -> null
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
