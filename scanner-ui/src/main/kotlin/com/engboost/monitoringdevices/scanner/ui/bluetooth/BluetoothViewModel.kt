package com.engboost.monitoringdevices.scanner.ui.bluetooth

import androidx.lifecycle.ViewModel
import com.engboost.monitoringdevices.scanner.bluetooth.BluetoothScanner
import com.engboost.monitoringdevices.scanner.ui.common.ScanStatus
import com.engboost.monitoringdevices.scanner.ui.common.isAllGranted
import com.engboost.monitoringdevices.scanner.ui.common.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class BluetoothViewModel(
    private val scanner: BluetoothScanner
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = mutableUiState.asStateFlow()

    fun onStartClick() {
        val missingPermissions = scanner.missingPermissions()
        mutableUiState.update { state ->
            if (missingPermissions.isNotEmpty()) {
                state.withPermissionRequest(missingPermissions)
            } else {
                state.copy(status = ScanStatus.Scanning, permissionRequest = null)
            }
        }
    }

    fun onStopClick() {
        mutableUiState.update { state ->
            state.copy(status = ScanStatus.Stopped, selectedDevice = null, permissionRequest = null)
        }
    }

    /**
     * Scans while the status is [ScanStatus.Scanning].
     *
     * The status is the only source of truth: Start and Stop change it, and a terminal scan event
     * replaces it, which cancels the scan. The caller cancels this function when the screen is hidden.
     */
    suspend fun scanWhileVisible() {
        mutableUiState
            .map { state -> state.isScanning }
            .distinctUntilChanged()
            .collectLatest { isScanning ->
                if (!isScanning) return@collectLatest

                scanner.scan()
                    .catch { error ->
                        val message = "Bluetooth scan error: ${error.toDisplayMessage()}"
                        mutableUiState.update { state -> state.copy(status = ScanStatus.Failed(message)) }
                    }
                    .collect { event -> mutableUiState.update { state -> state.reduce(event) } }
            }
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        if (isAllGranted(result)) {
            mutableUiState.update { state -> state.copy(permissionRequest = null) }
            onStartClick()
        } else {
            mutableUiState.update { state ->
                state.copy(status = ScanStatus.PermissionsDenied, permissionRequest = null)
            }
        }
    }

    fun onDeviceClick(device: BluetoothDeviceUi) {
        mutableUiState.update { state -> state.copy(selectedDevice = device) }
    }

    fun onDeviceDetailsDismiss() {
        mutableUiState.update { state -> state.copy(selectedDevice = null) }
    }

    fun onSortModeChange(sortMode: BluetoothSortMode) {
        mutableUiState.update { state -> state.withSortMode(sortMode) }
    }
}
