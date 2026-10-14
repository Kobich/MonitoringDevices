package com.engboost.monitoringdevices.monitoring.ui.wifi

import com.engboost.monitoringdevices.monitoring.ui.common.errorText
import com.engboost.monitoringdevices.monitoring.ui.common.nextPermissionRequest
import com.engboost.monitoringdevices.monitoring.wifi.WifiNetwork
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import com.engboost.monitoringdevices.monitoring.wifi.WifiUnavailableReason

/** Applies a scanner [event] to the screen state. */
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

        is WifiScanEvent.PermissionRequired -> withPermissionRequest(event.permissions)

        is WifiScanEvent.Unavailable -> copy(
            statusText = event.reason.toDisplayText(),
            networks = emptyList(),
            selectedNetwork = null,
            isScanning = false,
            permissionRequest = null
        )

        is WifiScanEvent.Error -> stopped(errorText(event.message, event.cause))
    }
}

internal fun WifiUiState.withPermissionRequest(permissions: Set<String>): WifiUiState {
    return copy(
        statusText = "Permissions required",
        isScanning = false,
        permissionRequest = nextPermissionRequest(permissionRequest, permissions)
    )
}

internal fun WifiUiState.stopped(statusText: String): WifiUiState {
    return copy(
        statusText = statusText,
        isScanning = false,
        permissionRequest = null
    )
}

private fun WifiUnavailableReason.toDisplayText(): String {
    return when (this) {
        WifiUnavailableReason.SOURCE_DISABLED -> "Wi-Fi scanning is disabled in the config"
        WifiUnavailableReason.WIFI_DISABLED -> "Wi-Fi is disabled"
        WifiUnavailableReason.LOCATION_DISABLED -> "Location Services are disabled"
        WifiUnavailableReason.SCAN_REJECTED ->
            "Wi-Fi scan was not started by Android. Check Wi-Fi, Location Services and scan throttling."
    }
}

private fun List<WifiNetwork>.toUiModels(): List<WifiNetworkUi> {
    return sortedByDescending { network -> network.rssiDbm }
        .map { network ->
            WifiNetworkUi(
                ssid = network.ssid ?: "Hidden network",
                bssid = network.bssid,
                rssiDbm = network.rssiDbm,
                frequencyMhz = network.frequencyMhz,
                capabilities = network.capabilities
            )
        }
}
