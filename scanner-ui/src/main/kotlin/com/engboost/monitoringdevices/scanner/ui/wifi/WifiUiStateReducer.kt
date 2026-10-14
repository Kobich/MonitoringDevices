package com.engboost.monitoringdevices.scanner.ui.wifi

import com.engboost.monitoringdevices.scanner.ui.common.ScanStatus
import com.engboost.monitoringdevices.scanner.ui.common.errorText
import com.engboost.monitoringdevices.scanner.ui.common.nextPermissionRequest
import com.engboost.monitoringdevices.scanner.wifi.ScannedWifiNetwork
import com.engboost.monitoringdevices.scanner.wifi.WifiScanEvent

/** Applies a scanner [event] to the screen state. */
internal fun WifiUiState.reduce(event: WifiScanEvent): WifiUiState {
    return when (event) {
        WifiScanEvent.Scanning -> copy(status = ScanStatus.Scanning)

        is WifiScanEvent.Networks -> {
            val networks = event.networks.toUiModels()
            copy(
                status = ScanStatus.Scanning,
                networks = networks,
                selectedNetwork = selectedNetwork?.let { selected ->
                    networks.find { network -> network.bssid == selected.bssid }
                }
            )
        }

        is WifiScanEvent.PermissionRequired -> withPermissionRequest(event.permissions)

        is WifiScanEvent.Unavailable -> copy(
            status = ScanStatus.Unavailable(event.reason),
            networks = emptyList(),
            selectedNetwork = null
        )

        is WifiScanEvent.Error -> copy(status = ScanStatus.Failed(errorText(event.message, event.cause)))
    }
}

internal fun WifiUiState.withPermissionRequest(permissions: Set<String>): WifiUiState {
    return copy(
        status = ScanStatus.PermissionsRequired,
        permissionRequest = nextPermissionRequest(permissionRequest, permissions)
    )
}

private fun List<ScannedWifiNetwork>.toUiModels(): List<WifiNetworkUi> {
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
