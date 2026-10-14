package com.engboost.monitoringdevices.monitoring.ui.wifi

internal data class WifiUiState(
    val statusText: String,
    val networks: List<WifiNetworkUi>,
    val selectedNetwork: WifiNetworkUi?,
    val isScanThrottlingDialogVisible: Boolean,
    val isScanning: Boolean,
    val permissionRequest: PermissionRequestUi?
)

internal data class WifiNetworkUi(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)

internal data class PermissionRequestUi(
    val id: Int,
    val permissions: List<String>
) {
    fun asArray(): Array<String> {
        return permissions.toTypedArray()
    }
}
