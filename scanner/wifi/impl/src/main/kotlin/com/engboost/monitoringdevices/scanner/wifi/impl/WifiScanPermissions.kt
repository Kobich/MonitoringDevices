package com.engboost.monitoringdevices.scanner.wifi.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal class WifiScanPermissions(
    private val appContext: Context
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

    fun missingPermissions(): Set<String> {
        return requiredPermissions
            .filter { permission ->
                appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
            }
            .toSet()
    }
}
