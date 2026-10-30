package com.android.appmanager.feature.conditiondatafetcher.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.android.appmanager.feature.conditiondatafetcher.ConditionFeature
import com.android.appmanager.feature.conditiondatafetcher.ConditionResult
import com.android.appmanager.feature.conditiondatafetcher.DataFeature
import com.android.appmanager.feature.conditiondatafetcher.Reading
import com.android.appmanager.feature.conditiondatafetcher.SourceId
import com.android.appmanager.feature.conditiondatafetcher.UnknownReason
import com.android.appmanager.feature.conditiondatafetcher.WifiFoundCondition
import com.android.appmanager.feature.conditiondatafetcher.WifiFoundData
import com.android.appmanager.feature.conditiondatafetcher.WifiNetworkCondition
import com.android.appmanager.feature.conditiondatafetcher.valueOrNull

/** Wi-Fi networks from the system scan cache; never starts a scan. */
class WifiNetworksFeature(context: Context) :
    DataFeature<WifiFoundData>,
    ConditionFeature<WifiFoundCondition> {

    override fun getId() = SourceId.WIFI_FOUND

    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)

    override suspend fun getData(): WifiFoundData? = readNetworks().valueOrNull()?.let(::WifiFoundData)

    override suspend fun isMeetsCondition(input: WifiFoundCondition): ConditionResult {
        if (input.networks.isEmpty()) return ConditionResult.False

        val detected = when (val reading = readNetworks()) {
            is Reading.Failure -> return ConditionResult.Unknown(reading.reason)
            is Reading.Success -> reading.value
        }
        // An empty cache usually means there was no scan yet, not that there are no networks.
        if (detected.isEmpty()) return ConditionResult.Unknown(UnknownReason.NO_DATA)

        return ConditionResult.of(input.networks.any { wanted -> detected.any { wanted.matches(it) } })
    }

    private fun WifiNetworkCondition.matches(network: WifiNetwork): Boolean {
        if (name != network.ssid) return false
        return bssid == null || bssid.equals(network.bssid, ignoreCase = true)
    }

    // Without location permission or with location off Android returns an empty list, not an error,
    // so these are checked first to tell them apart from an empty cache.
    private fun readNetworks(): Reading<List<WifiNetwork>> {
        val manager = wifiManager ?: return Reading.Failure(UnknownReason.SOURCE_UNAVAILABLE)
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Reading.Failure(UnknownReason.PERMISSION_DENIED)
        }
        if (!manager.isWifiEnabled) return Reading.Failure(UnknownReason.SOURCE_DISABLED)
        if (appContext.getSystemService(LocationManager::class.java)?.isLocationEnabled != true) {
            return Reading.Failure(UnknownReason.SOURCE_DISABLED)
        }
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
