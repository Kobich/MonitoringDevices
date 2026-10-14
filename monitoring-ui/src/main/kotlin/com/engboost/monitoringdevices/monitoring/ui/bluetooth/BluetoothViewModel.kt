package com.engboost.monitoringdevices.monitoring.ui.bluetooth

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

internal class BluetoothViewModel(
    private val monitoring: DeviceMonitoring
) : ViewModel() {
    /** Whether the user pressed Start; the scan itself runs only while the screen is visible. */
    private val isScanRequested = MutableStateFlow(false)

    var uiState by mutableStateOf(BluetoothUiState())
        private set

    fun onStartClick() {
        val missingPermissions = monitoring.missingPermissions(MonitoringSource.BLUETOOTH)
        if (missingPermissions.isNotEmpty()) {
            uiState = uiState.withPermissionRequest(missingPermissions)
            return
        }

        uiState = uiState.copy(statusText = "Starting Bluetooth scan", isScanning = true, permissionRequest = null)
        isScanRequested.value = true
    }

    fun onStopClick() {
        isScanRequested.value = false
        uiState = uiState.stopped("Stopped").copy(selectedDevice = null)
    }

    /** Scans while the user wants it; the caller cancels it when the screen is not visible. */
    suspend fun scanWhileRequested() {
        isScanRequested.collectLatest { isRequested ->
            if (!isRequested) return@collectLatest

            monitoring.scanBluetooth()
                .catch { error -> uiState = uiState.stopped("Bluetooth scan error: ${error.toDisplayMessage()}") }
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

    fun onDeviceClick(device: BluetoothDeviceUi) {
        uiState = uiState.copy(selectedDevice = device)
    }

    fun onDeviceDetailsDismiss() {
        uiState = uiState.copy(selectedDevice = null)
    }

    fun onSortModeChange(sortMode: BluetoothSortMode) {
        uiState = uiState.withSortMode(sortMode)
    }
}
