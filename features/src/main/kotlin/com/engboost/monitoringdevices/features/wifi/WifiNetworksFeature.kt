package com.engboost.monitoringdevices.features.wifi

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.engboost.monitoringdevices.features.ConditionFeature
import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.DataFeature

/**
 * Wi-Fi networks from the system scan cache. Does not start a new scan.
 *
 * [getData] returns null when Wi-Fi is unavailable or permissions are missing.
 * [isMeetsCondition] checks whether any network with one of the given names (SSID) is detected.
 */
class WifiNetworksFeature(context: Context) :
    DataFeature<List<WifiNetwork>>,
    ConditionFeature<List<String>> {

    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)

    override suspend fun getData(): List<WifiNetwork>? {
        val manager = wifiManager ?: return null
        return try {
            manager.scanResults.map { it.toWifiNetwork() }
        } catch (error: SecurityException) {
            null
        }
    }

    override suspend fun isMeetsCondition(input: List<String>): ConditionResult {
        // An empty cache usually means there was no scan yet, not that there are no networks.
        val detected = getData()?.takeIf { it.isNotEmpty() } ?: return ConditionResult.UNKNOWN

        val detectedNames = detected.mapNotNullTo(HashSet()) { it.ssid }
        val isAnyDetected = input.any { it in detectedNames }
        return if (isAnyDetected) ConditionResult.TRUE else ConditionResult.FALSE
    }

    @Suppress("DEPRECATION")
    private fun ScanResult.toWifiNetwork() = WifiNetwork(
        ssid = SSID?.takeIf { it.isNotEmpty() },
        bssid = BSSID,
        rssiDbm = level,
        frequencyMhz = frequency,
        capabilities = capabilities
    )
}
