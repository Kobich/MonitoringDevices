package com.engboost.monitoringdevices.features.wifi

import com.engboost.monitoringdevices.features.DataFeature
import com.engboost.monitoringdevices.features.FeatureResult
import com.engboost.monitoringdevices.scanner.wifi.api.WifiNetwork
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanConfig
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanMode
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull

/** List of Wi-Fi networks taken from the system scan cache, without triggering a new scan. */
class WifiNetworksFeature(
    private val wifiScanner: WifiScanner
) : DataFeature<List<WifiNetwork>> {
    override val id: String = ID

    override suspend fun getData(): FeatureResult<List<WifiNetwork>> {
        val event = wifiScanner.scan(WifiScanConfig(mode = WifiScanMode.CACHED_ONLY))
            .filterNot { it is WifiScanEvent.Scanning }
            .firstOrNull()
            ?: return FeatureResult.Unavailable("Wi-Fi scanner returned no data")

        return when (event) {
            is WifiScanEvent.Networks -> FeatureResult.Success(event.networks)
            is WifiScanEvent.PermissionRequired -> FeatureResult.PermissionRequired(event.permissions)
            is WifiScanEvent.Unavailable -> FeatureResult.Unavailable(event.reason)
            is WifiScanEvent.Error -> FeatureResult.Error(event.message, event.cause)
            WifiScanEvent.Scanning -> FeatureResult.Unavailable("Wi-Fi scan is in progress")
        }
    }

    companion object {
        const val ID = "wifi.networks"
    }
}
