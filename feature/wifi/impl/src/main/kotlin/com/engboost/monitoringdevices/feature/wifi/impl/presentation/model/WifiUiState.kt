package com.engboost.monitoringdevices.feature.wifi.impl.presentation.model

data class WifiUiState(
    val statusText: String,
    val networks: List<WifiNetworkUi>,
    val selectedNetwork: WifiNetworkUi?,
    val scanThrottlingDialog: WifiScanThrottlingHintUi?,
    val isScanning: Boolean,
    val permissionRequest: PermissionRequestUi?
)

data class WifiNetworkUi(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)

data class WifiScanThrottlingHintUi(
    val title: String,
    val message: String,
    val actionText: String?
)

data class PermissionRequestUi(
    val id: Int,
    val permissions: List<String>
) {
    fun asArray(): Array<String> {
        return permissions.toTypedArray()
    }
}
