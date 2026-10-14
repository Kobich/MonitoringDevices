package com.engboost.monitoringdevices.monitoring.ui.ble

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

internal class BleViewModel(
    private val monitoring: DeviceMonitoring,
    private val uiStateReducer: BleUiStateReducer = BleUiStateReducer(BleDeviceUiMapper())
) : ViewModel() {
    private var eventsJob: Job? = null

    var uiState by mutableStateOf(
        BleUiState(
            statusText = "Stopped",
            devices = emptyList(),
            selectedDevice = null,
            sortMode = BleSortMode.STABLE,
            isScanning = false,
            permissionRequest = null
        )
    )
        private set

    init {
        if (MonitoringSource.BLE in monitoring.state.value.activeSources) {
            observeEvents("BLE scan running")
        }
    }

    fun onStartClick() {
        if (eventsJob?.isActive == true) return

        when (val result = monitoring.start(MonitoringSource.BLE)) {
            StartResult.Started -> observeEvents("Starting BLE scan")
            is StartResult.PermissionsRequired -> {
                uiState = uiStateReducer.withPermissionRequest(uiState, result.permissions)
            }
            StartResult.SourceDisabled -> {
                uiState = uiState.copy(statusText = "BLE monitoring is disabled", isScanning = false)
            }
            is StartResult.Failed -> {
                uiState = uiState.copy(
                    statusText = "Failed to start BLE monitoring: ${result.cause.toDisplayMessage()}",
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
            monitoring.bleEvents()
                .catch { error ->
                    uiState = uiState.copy(
                        statusText = "BLE scan error: ${error.toDisplayMessage()}",
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
                    uiState = uiStateReducer.reduce(uiState, event)
                }
        }
        eventsJob = job
        job.start()
    }

    fun onStopClick() {
        monitoring.stop(MonitoringSource.BLE)
        eventsJob?.cancel()
        eventsJob = null
        uiState = uiState.copy(
            statusText = "Stopped",
            selectedDevice = null,
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

    fun onDeviceClick(device: BleDeviceUi) {
        uiState = uiState.copy(selectedDevice = device)
    }

    fun onDeviceDetailsDismiss() {
        uiState = uiState.copy(selectedDevice = null)
    }

    fun onSortModeChange(sortMode: BleSortMode) {
        uiState = uiStateReducer.changeSortMode(uiState, sortMode)
    }
}
