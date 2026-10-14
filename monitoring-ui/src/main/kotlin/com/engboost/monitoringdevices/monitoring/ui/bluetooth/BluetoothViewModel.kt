package com.engboost.monitoringdevices.monitoring.ui.bluetooth

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

internal class BluetoothViewModel(
    private val monitoring: DeviceMonitoring
) : ViewModel() {
    private var eventsJob: Job? = null

    var uiState by mutableStateOf(BluetoothUiState())
        private set

    init {
        // Monitoring keeps running in the service while the screen is closed.
        if (MonitoringSource.BLUETOOTH in monitoring.state.value.activeSources) {
            observeEvents()
        }
    }

    fun onStartClick() {
        if (eventsJob?.isActive == true) return

        when (val result = monitoring.start(MonitoringSource.BLUETOOTH)) {
            StartResult.Started -> observeEvents()
            is StartResult.PermissionsRequired -> uiState = uiState.withPermissionRequest(result.permissions)
            StartResult.SourceDisabled -> uiState = uiState.stopped("Bluetooth monitoring is disabled")
            is StartResult.Failed -> uiState = uiState.stopped(
                "Failed to start Bluetooth monitoring: ${result.cause.toDisplayMessage()}"
            )
        }
    }

    fun onStopClick() {
        monitoring.stop(MonitoringSource.BLUETOOTH)
        eventsJob?.cancel()
        eventsJob = null
        uiState = uiState.stopped("Stopped").copy(selectedDevice = null)
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        if (isAllGranted(result)) {
            uiState = uiState.copy(permissionRequest = null)
            onStartClick()
        } else {
            uiState = uiState.stopped("Permissions denied")
        }
    }

    fun onDeviceClick(device: BluetoothDeviceUi) {
        uiState = uiState.copy(selectedDevice = device)
    }

    fun onDeviceDetailsDismiss() {
        uiState = uiState.copy(selectedDevice = null)
    }

    fun onSortModeChange(sortMode: BluetoothSortMode) {
        uiState = uiState.withSortMode(sortMode)
    }

    private fun observeEvents() {
        uiState = uiState.copy(statusText = "Starting Bluetooth scan", isScanning = true, permissionRequest = null)

        // Started lazily: the flow may complete immediately, and the handler must see eventsJob set.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            monitoring.bluetoothEvents()
                .catch { error -> uiState = uiState.stopped("Bluetooth scan error: ${error.toDisplayMessage()}") }
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
