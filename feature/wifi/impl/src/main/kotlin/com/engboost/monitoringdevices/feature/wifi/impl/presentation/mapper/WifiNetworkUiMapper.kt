package com.engboost.monitoringdevices.feature.wifi.impl.presentation.mapper

import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiNetworkUi
import com.engboost.monitoringdevices.scanner.wifi.api.WifiNetwork

internal class WifiNetworkUiMapper {
    fun map(networks: List<WifiNetwork>): List<WifiNetworkUi> {
        return networks
            .sortedByDescending { network -> network.rssiDbm }
            .map { network ->
                WifiNetworkUi(
                    ssid = network.ssid?.takeIf { ssid -> ssid.isNotBlank() } ?: "Hidden network",
                    bssid = network.bssid,
                    rssiDbm = network.rssiDbm,
                    frequencyMhz = network.frequencyMhz,
                    capabilities = network.capabilities
                )
            }
    }
}
