package com.engboost.monitoringdevices.features.wifi

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.engboost.monitoringdevices.features.ConditionFeature
import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.DataFeature
import com.engboost.monitoringdevices.features.Reading
import com.engboost.monitoringdevices.features.UnknownReason
import com.engboost.monitoringdevices.features.valueOrNull

/**
 * Wi-Fi networks from the system scan cache. Does not start a new scan.
 *
 * [getData] returns null when Wi-Fi is unavailable or permissions are missing.
 * [isMeetsCondition] checks whether any network with one of the given names (SSID) is detected.
 *
 * Unknown reasons: [UnknownReason.SOURCE_UNAVAILABLE], [UnknownReason.PERMISSION_DENIED],
 * [UnknownReason.NO_DATA] (the cache is empty).
 */
class WifiNetworksFeature(context: Context) :
    DataFeature<List<WifiNetwork>>,
    ConditionFeature<List<String>> {

    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)

    override suspend fun getData(): List<WifiNetwork>? = readNetworks().valueOrNull()

    override suspend fun isMeetsCondition(input: List<String>): ConditionResult {
        val detected = when (val reading = readNetworks()) {
            is Reading.Failure -> return ConditionResult.Unknown(reading.reason)
            is Reading.Success -> reading.value
        }
        // An empty cache usually means there was no scan yet, not that there are no networks.
        if (detected.isEmpty()) return ConditionResult.Unknown(UnknownReason.NO_DATA)

        val detectedNames = detected.mapNotNullTo(HashSet()) { it.ssid }
        return ConditionResult.of(input.any { it in detectedNames })
    }

    private fun readNetworks(): Reading<List<WifiNetwork>> {
        val manager = wifiManager ?: return Reading.Failure(UnknownReason.SOURCE_UNAVAILABLE)
        return try {
            Reading.Success(manager.scanResults.map { it.toWifiNetwork() })
        } catch (error: SecurityException) {
            Reading.Failure(UnknownReason.PERMISSION_DENIED)
        }
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
