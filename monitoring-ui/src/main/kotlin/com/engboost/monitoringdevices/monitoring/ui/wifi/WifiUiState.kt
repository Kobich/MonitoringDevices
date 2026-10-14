package com.engboost.monitoringdevices.monitoring.ui.wifi

import com.engboost.monitoringdevices.monitoring.ui.common.PermissionRequestUi

internal data class WifiUiState(
    val statusText: String = "Stopped",
    val networks: List<WifiNetworkUi> = emptyList(),
    val selectedNetwork: WifiNetworkUi? = null,
    val isScanThrottlingDialogVisible: Boolean = false,
    val isScanning: Boolean = false,
    val permissionRequest: PermissionRequestUi? = null
)

internal data class WifiNetworkUi(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)
