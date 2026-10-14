package com.engboost.monitoringdevices.monitoring.ui.wifi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.MonitoringSource
import com.engboost.monitoringdevices.monitoring.StartResult
import com.engboost.monitoringdevices.monitoring.ui.common.isAllGranted
import com.engboost.monitoringdevices.monitoring.ui.common.toDisplayMessage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class WifiViewModel(
    private val monitoring: DeviceMonitoring
) : ViewModel() {
    private var eventsJob: Job? = null

    var uiState by mutableStateOf(WifiUiState())
        private set

    init {
        // Monitoring keeps running in the service while the screen is closed.
        if (MonitoringSource.WIFI in monitoring.state.value.activeSources) {
            observeEvents()
        }
    }

    fun onStartClick() {
        if (eventsJob?.isActive == true) return

        refreshThrottlingStatus(showDialog = true)
        when (val result = monitoring.start(MonitoringSource.WIFI)) {
            StartResult.Started -> observeEvents()
            is StartResult.PermissionsRequired -> uiState = uiState.withPermissionRequest(result.permissions)
            StartResult.SourceDisabled -> uiState = uiState.stopped("Wi-Fi monitoring is disabled")
            is StartResult.Failed -> uiState = uiState.stopped(
                "Failed to start Wi-Fi monitoring: ${result.cause.toDisplayMessage()}"
            )
        }
    }

    fun onStopClick() {
        monitoring.stop(MonitoringSource.WIFI)
        eventsJob?.cancel()
        eventsJob = null
        uiState = uiState.stopped("Stopped").copy(selectedNetwork = null)
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

    private fun observeEvents() {
        uiState = uiState.copy(statusText = "Starting Wi-Fi scan", isScanning = true, permissionRequest = null)

        // Started lazily: the flow may complete immediately, and the handler must see eventsJob set.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            monitoring.wifiEvents()
                .catch { error -> uiState = uiState.stopped("Wi-Fi scan error: ${error.toDisplayMessage()}") }
                .collect { event -> uiState = uiState.reduce(event) }
        }
        eventsJob = job
        job.invokeOnCompletion {
            // The session ended without a terminal event, e.g. stopped from the notification.
            if (eventsJob === job) {
                eventsJob = null
                if (uiState.isScanning) uiState = uiState.stopped("Stopped")
            }
        }
        job.start()
    }
}
