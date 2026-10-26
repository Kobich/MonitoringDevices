package com.engboost.monitoringdevices.scanner.ui.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.scanner.bluetooth.BluetoothScannerApi
import com.engboost.monitoringdevices.scanner.ui.common.ScanStatus
import com.engboost.monitoringdevices.scanner.ui.common.isAllGranted
import com.engboost.monitoringdevices.scanner.ui.common.toDisplayMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal class BluetoothViewModel(
    private val scanner: BluetoothScannerApi
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = mutableUiState.asStateFlow()

    /** Becomes false only [SCAN_STOP_DELAY] after the screen is hidden. */
    private val isScreenVisible = MutableStateFlow(false)
    private var hideJob: Job? = null

    /**
     * Scans while the status is [ScanStatus.Scanning] and the screen is visible.
     *
     * The status is the only source of truth: Start and Stop change it, and a terminal scan event
     * replaces it, which cancels the scan.
     *
     * The scan lives in the view model, not in the screen, so recreating the screen on rotation
     * does not restart it. Restarting within 8 seconds would hit the scanner's start limit.
     */
    init {
        viewModelScope.launch {
            combine(
                mutableUiState.map { state -> state.isScanning },
                isScreenVisible
            ) { isScanning, isVisible -> isScanning && isVisible }
                .distinctUntilChanged()
                .collectLatest { shouldScan -> if (shouldScan) scan() }
        }
    }

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

    /** A short hide, such as a rotation, keeps the scan running. */
    fun onScreenVisibilityChange(isVisible: Boolean) {
        hideJob?.cancel()
        if (isVisible) {
            isScreenVisible.value = true
        } else {
            hideJob = viewModelScope.launch {
                delay(SCAN_STOP_DELAY)
                isScreenVisible.value = false
            }
        }
    }

    private suspend fun scan() {
        scanner.scan()
            .catch { error ->
                val message = "Bluetooth scan error: ${error.toDisplayMessage()}"
                mutableUiState.update { state -> state.copy(status = ScanStatus.Failed(message)) }
            }
            .collect { event -> mutableUiState.update { state -> state.reduce(event) } }
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

    private companion object {
        val SCAN_STOP_DELAY = 5.seconds
    }
}
