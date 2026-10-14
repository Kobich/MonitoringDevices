package com.engboost.monitoringdevices.monitoring.ui.wifi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.MonitoringSource
import com.engboost.monitoringdevices.monitoring.StartResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

internal class WifiViewModel(
    private val monitoring: DeviceMonitoring
) : ViewModel() {
    private var eventsJob: Job? = null

    var uiState by mutableStateOf(
        WifiUiState(
            statusText = "Stopped",
            networks = emptyList(),
            selectedNetwork = null,
            isScanThrottlingDialogVisible = false,
            isScanning = false,
            permissionRequest = null
        )
    )
        private set

    init {
        if (MonitoringSource.WIFI in monitoring.state.value.activeSources) {
            observeEvents("Wi-Fi scan running")
        }
    }

    fun onStartClick() {
        if (eventsJob?.isActive == true) return

        refreshThrottlingStatus(showDialog = true)
        when (val result = monitoring.start(MonitoringSource.WIFI)) {
            StartResult.Started -> observeEvents("Starting Wi-Fi scan")
            is StartResult.PermissionsRequired -> {
                uiState = uiState.withPermissionRequest(result.permissions)
            }
            StartResult.SourceDisabled -> {
                uiState = uiState.copy(statusText = "Wi-Fi monitoring is disabled", isScanning = false)
            }
            is StartResult.Failed -> {
                uiState = uiState.copy(
                    statusText = "Failed to start Wi-Fi monitoring: ${result.cause.toDisplayMessage()}",
                    isScanning = false
                )
            }
        }
    }

    private fun observeEvents(statusText: String) {
        uiState = uiState.copy(
            statusText = statusText,
            isScanning = true,
            permissionRequest = null
        )

        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            monitoring.wifiEvents()
                .catch { error ->
                    uiState = uiState.copy(
                        statusText = "Wi-Fi scan error: ${error.toDisplayMessage()}",
                        isScanning = false,
                        permissionRequest = null
                    )
                }
                .onCompletion {
                    if (eventsJob === job) {
                        eventsJob = null
                        if (uiState.isScanning) {
                            uiState = uiState.copy(
                                statusText = "Stopped",
                                isScanning = false,
                                permissionRequest = null
                            )
                        }
                    }
                }
                .collect { event ->
                    uiState = uiState.reduce(event)
                }
        }
        eventsJob = job
        job.start()
    }

    fun onStopClick() {
        monitoring.stop(MonitoringSource.WIFI)
        eventsJob?.cancel()
        eventsJob = null
        uiState = uiState.copy(
            statusText = "Stopped",
            selectedNetwork = null,
            isScanning = false,
            permissionRequest = null
        )
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        val deniedPermissions = result
            .filterValues { isGranted -> !isGranted }
            .keys

        if (result.isNotEmpty() && deniedPermissions.isEmpty()) {
            uiState = uiState.copy(permissionRequest = null)
            onStartClick()
        } else {
            uiState = uiState.copy(
                statusText = "Permissions denied",
                isScanning = false,
                permissionRequest = null
            )
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

    /** The service re-reads throttling on every scan cycle, so only the dialog needs an update. */
    fun refreshThrottlingStatus(showDialog: Boolean = false) {
        uiState = uiState.copy(
            isScanThrottlingDialogVisible = monitoring.isWifiScanThrottlingEnabled &&
                (showDialog || uiState.isScanThrottlingDialogVisible)
        )
    }
}
