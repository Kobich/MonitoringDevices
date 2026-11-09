package com.android.appmanager.feature.scanner.ui.wifi

import com.android.appmanager.feature.scanner.ui.common.PermissionRequestUi
import com.android.appmanager.feature.scanner.ui.common.ScanStatus
import com.android.appmanager.feature.scanner.wifi.WifiUnavailableReason

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
