package com.engboost.monitoringdevices.scanner.ui.wifi

import com.engboost.monitoringdevices.scanner.ui.common.PermissionRequestUi
import com.engboost.monitoringdevices.scanner.ui.common.ScanStatus
import com.engboost.monitoringdevices.scanner.wifi.WifiUnavailableReason

internal data class WifiUiState(
    val status: ScanStatus<WifiUnavailableReason> = ScanStatus.Stopped,
    val networks: List<WifiNetworkUi> = emptyList(),
    val selectedNetwork: WifiNetworkUi? = null,
    val isScanThrottlingDialogVisible: Boolean = false,
    val permissionRequest: PermissionRequestUi? = null
) {
    val isScanning: Boolean
        get() = status == ScanStatus.Scanning
}

internal data class WifiNetworkUi(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)
