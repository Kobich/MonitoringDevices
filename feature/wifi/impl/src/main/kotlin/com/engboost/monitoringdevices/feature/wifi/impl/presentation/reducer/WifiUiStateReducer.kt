package com.engboost.monitoringdevices.feature.wifi.impl.presentation.reducer

import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.PermissionRequestUi
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiNetworkUi
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiUiState
import com.engboost.monitoringdevices.scanner.wifi.api.WifiNetwork
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent

internal fun WifiUiState.reduce(event: WifiScanEvent): WifiUiState {
    return when (event) {
        WifiScanEvent.Scanning -> copy(
            statusText = "Wi-Fi scan running",
            isScanning = true,
            permissionRequest = null
        )

        is WifiScanEvent.Networks -> {
            val networks = event.networks.toUiModels()
            copy(
                statusText = "Wi-Fi data received",
                networks = networks,
                selectedNetwork = selectedNetwork?.let { selected ->
                    networks.find { network -> network.bssid == selected.bssid }
                },
                isScanning = true,
                permissionRequest = null
            )
        }

        is WifiScanEvent.PermissionRequired -> copy(
            statusText = "Permissions required",
            isScanning = false,
            permissionRequest = PermissionRequestUi(event.permissions.toList())
        )

        is WifiScanEvent.Unavailable -> copy(
            statusText = event.reason,
            networks = emptyList(),
            selectedNetwork = null,
            isScanning = false,
            permissionRequest = null
        )

        is WifiScanEvent.Error -> copy(
            statusText = event.cause
                ?.let { cause -> "${event.message}: ${cause.toDisplayMessage()}" }
                ?: event.message,
            isScanning = false,
            permissionRequest = null
        )
    }
}

private fun List<WifiNetwork>.toUiModels(): List<WifiNetworkUi> {
    return sortedByDescending { network -> network.rssiDbm }
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

private fun Throwable.toDisplayMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}
