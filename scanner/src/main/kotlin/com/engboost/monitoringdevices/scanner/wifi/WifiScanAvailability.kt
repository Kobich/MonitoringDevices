package com.engboost.monitoringdevices.scanner.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

/** Checks permissions and device state required for Wi-Fi scanning. */
internal class WifiScanAvailability(
    private val context: Context,
    private val wifiManager: WifiManager
) {
    val requiredPermissions: Set<String> = buildSet {
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.CHANGE_WIFI_STATE)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    /** Whether Android scan throttling is enabled; assumes `true` when the status is unknown. */
    val isScanThrottlingEnabled: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return wifiManager.isScanThrottleEnabled
            return try {
                Settings.Global.getInt(context.contentResolver, "wifi_scan_throttle_enabled") == 1
            } catch (_: Exception) {
                true
            }
        }

    fun missingPermissions(): Set<String> {
        return requiredPermissions.filterTo(linkedSetOf()) { permission ->
            context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /** Returns why scanning is impossible right now, or `null` if it can start. */
    fun unavailableReason(): WifiUnavailableReason? {
        return when {
            !wifiManager.isWifiEnabled -> WifiUnavailableReason.WIFI_DISABLED
            !isLocationEnabled() -> WifiUnavailableReason.LOCATION_DISABLED
            else -> null
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(LocationManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }
    }
}
