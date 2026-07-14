package com.engboost.monitoringdevices.scanner.wifi.impl

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.engboost.monitoringdevices.scanner.wifi.api.WifiNetwork

internal class WifiScanResultReader(
    private val wifiManager: WifiManager
) {
    @SuppressLint("MissingPermission")
    fun readScanResults(): List<WifiNetwork> {
        return runCatching {
            wifiManager.scanResults.map { result ->
                WifiNetwork(
                    ssid = result.readSsid(),
                    bssid = result.BSSID,
                    rssiDbm = result.level,
                    frequencyMhz = result.frequency,
                    capabilities = result.capabilities
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun ScanResult.readSsid(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wifiSsid?.toString()?.takeIf { it.isNotBlank() }
        } else {
            @Suppress("DEPRECATION")
            SSID.takeIf { it.isNotBlank() }
        }
    }
}
