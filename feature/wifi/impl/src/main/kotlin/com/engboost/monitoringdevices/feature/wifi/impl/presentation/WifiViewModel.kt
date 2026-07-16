package com.engboost.monitoringdevices.feature.wifi.impl.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.feature.wifi.impl.domain.interactor.WifiScanInteractor
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiNetworkUi
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiUiState
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.reducer.reduce
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

internal class WifiViewModel(
    private val scanInteractor: WifiScanInteractor
) : ViewModel() {
    private var scanJob: Job? = null
    private var isScanThrottlingEnabled = scanInteractor.isScanThrottlingEnabled

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

    fun onStartClick() {
        if (scanJob?.isActive == true) return

        refreshThrottlingStatus(showDialog = true)
        startScan("Starting Wi-Fi scan")
    }

    private fun startScan(statusText: String) {
        uiState = uiState.copy(
            statusText = statusText,
            isScanning = true,
            permissionRequest = null
        )

        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            scanInteractor.scan()
                .catch {
                    uiState = uiState.copy(
                        statusText = "Wi-Fi scan error",
                        isScanning = false,
                        permissionRequest = null
                    )
                }
                .onCompletion {
                    if (scanJob === job) {
                        scanJob = null
                    }
                }
                .collect { result ->
                    uiState = uiState.reduce(result)
                }
        }
        scanJob = job
        job.start()
    }

    fun onStopClick() {
        scanJob?.cancel()
        scanJob = null
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

    fun refreshThrottlingStatus(showDialog: Boolean = false) {
        val previousStatus = isScanThrottlingEnabled
        val currentStatus = scanInteractor.isScanThrottlingEnabled

        isScanThrottlingEnabled = currentStatus
        uiState = uiState.copy(
            isScanThrottlingDialogVisible = currentStatus &&
                (showDialog || uiState.isScanThrottlingDialogVisible)
        )

        if (previousStatus != currentStatus && scanJob?.isActive == true) {
            restartScan()
        }
    }

    private fun restartScan() {
        scanJob?.cancel()
        startScan("Restarting Wi-Fi scan")
    }
}
