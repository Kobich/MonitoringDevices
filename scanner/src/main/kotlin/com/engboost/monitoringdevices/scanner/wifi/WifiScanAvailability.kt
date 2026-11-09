package com.engboost.monitoringdevices.scanner.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager

internal class WifiScanAvailability(
    private val context: Context,
    private val wifiManager: WifiManager
) {
    val requiredPermissions: Set<String> = setOf(
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val isScanThrottlingEnabled: Boolean
        get() = wifiManager.isScanThrottleEnabled

    fun missingPermissions(): Set<String> {
        return requiredPermissions.filterTo(linkedSetOf()) { permission ->
            context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun unavailableReason(): WifiUnavailableReason? {
        return when {
            !wifiManager.isWifiEnabled -> WifiUnavailableReason.WIFI_DISABLED
            context.getSystemService(LocationManager::class.java)?.isLocationEnabled != true ->
                WifiUnavailableReason.LOCATION_DISABLED
            else -> null
        }
    }
}
