package com.engboost.monitoringdevices.scanner.ui.wifi

import androidx.lifecycle.ViewModel
import com.engboost.monitoringdevices.scanner.ui.common.ScanStatus
import com.engboost.monitoringdevices.scanner.ui.common.isAllGranted
import com.engboost.monitoringdevices.scanner.ui.common.toDisplayMessage
import com.engboost.monitoringdevices.scanner.wifi.WifiScannerApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class WifiViewModel(
    private val scanner: WifiScannerApi
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = mutableUiState.asStateFlow()

    fun onStartClick() {
        refreshThrottlingStatus(showDialog = true)

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
            state.copy(status = ScanStatus.Stopped, selectedNetwork = null, permissionRequest = null)
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
                        val message = "Wi-Fi scan error: ${error.toDisplayMessage()}"
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

    fun onNetworkClick(network: WifiNetworkUi) {
        mutableUiState.update { state -> state.copy(selectedNetwork = network) }
    }

    fun onNetworkDetailsDismiss() {
        mutableUiState.update { state -> state.copy(selectedNetwork = null) }
    }

    fun onScanThrottlingDialogDismiss() {
        mutableUiState.update { state -> state.copy(isScanThrottlingDialogVisible = false) }
    }

    /** Only updates the dialog: the scanner picks up the new interval on its next cycle. */
    fun refreshThrottlingStatus(showDialog: Boolean = false) {
        val isThrottlingEnabled = scanner.isScanThrottlingEnabled
        mutableUiState.update { state ->
            state.copy(
                isScanThrottlingDialogVisible = isThrottlingEnabled &&
                    (showDialog || state.isScanThrottlingDialogVisible)
            )
        }
    }
}
