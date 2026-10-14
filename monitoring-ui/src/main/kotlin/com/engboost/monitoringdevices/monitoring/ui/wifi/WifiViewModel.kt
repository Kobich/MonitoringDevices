package com.engboost.monitoringdevices.monitoring.ui.wifi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.MonitoringSource
import com.engboost.monitoringdevices.monitoring.ui.common.isAllGranted
import com.engboost.monitoringdevices.monitoring.ui.common.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

internal class WifiViewModel(
    private val monitoring: DeviceMonitoring
) : ViewModel() {
    /** Whether the user pressed Start; the scan itself runs only while the screen is visible. */
    private val isScanRequested = MutableStateFlow(false)

    var uiState by mutableStateOf(WifiUiState())
        private set

    fun onStartClick() {
        refreshThrottlingStatus(showDialog = true)

        val missingPermissions = monitoring.missingPermissions(MonitoringSource.WIFI)
        if (missingPermissions.isNotEmpty()) {
            uiState = uiState.withPermissionRequest(missingPermissions)
            return
        }

        uiState = uiState.copy(statusText = "Starting Wi-Fi scan", isScanning = true, permissionRequest = null)
        isScanRequested.value = true
    }

    fun onStopClick() {
        isScanRequested.value = false
        uiState = uiState.stopped("Stopped").copy(selectedNetwork = null)
    }

    /** Scans while the user wants it; the caller cancels it when the screen is not visible. */
    suspend fun scanWhileRequested() {
        isScanRequested.collectLatest { isRequested ->
            if (!isRequested) return@collectLatest

            monitoring.scanWifi()
                .catch { error -> uiState = uiState.stopped("Wi-Fi scan error: ${error.toDisplayMessage()}") }
                .collect { event -> uiState = uiState.reduce(event) }
            // The flow completes only after a terminal event, which has already updated the state.
            isScanRequested.value = false
        }
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        if (isAllGranted(result)) {
            uiState = uiState.copy(permissionRequest = null)
            onStartClick()
        } else {
            uiState = uiState.stopped("Permissions denied")
        }
    }

    fun onNetworkClick(network: WifiNetworkUi) {
        uiState = uiState.copy(selectedNetwork = network)
    }

    fun onNetworkDetailsDismiss() {
        uiState = uiState.copy(selectedNetwork = null)
    }

    fun onScanThrottlingDialogDismiss() {
        uiState = uiState.copy(isScanThrottlingDialogVisible = false)
    }

    /**
     * Shows the throttling dialog if throttling is enabled and the dialog is requested or already visible.
     *
     * The scan interval itself is updated by the library on the next scan cycle.
     */
    fun refreshThrottlingStatus(showDialog: Boolean = false) {
        uiState = uiState.copy(
            isScanThrottlingDialogVisible = monitoring.isWifiScanThrottlingEnabled &&
                (showDialog || uiState.isScanThrottlingDialogVisible)
        )
    }
}
