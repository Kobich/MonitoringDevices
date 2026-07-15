package com.engboost.monitoringdevices.feature.wifi.impl.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.feature.wifi.impl.domain.interactor.WifiScanInteractor
import com.engboost.monitoringdevices.feature.wifi.impl.domain.model.WifiScanThrottling
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.mapper.WifiScanThrottlingUiMapper
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiNetworkUi
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiUiState
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.reducer.WifiUiStateReducer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

internal class WifiViewModel(
    private val scanInteractor: WifiScanInteractor,
    private val uiStateReducer: WifiUiStateReducer,
    private val scanThrottlingUiMapper: WifiScanThrottlingUiMapper
) : ViewModel() {
    private var scanJob: Job? = null
    private var scanThrottling: WifiScanThrottling = scanInteractor.scanThrottling

    var uiState by mutableStateOf(
        WifiUiState(
            statusText = "Stopped",
            networks = emptyList(),
            selectedNetwork = null,
            scanThrottlingDialog = null,
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
                    uiState = uiStateReducer.reduce(uiState, result)
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
        uiState = uiState.copy(scanThrottlingDialog = null)
    }

    fun refreshThrottlingStatus(showDialog: Boolean = false) {
        val previousStatus = scanThrottling
        val currentStatus = scanInteractor.scanThrottling

        scanThrottling = currentStatus
        uiState = uiState.copy(
            scanThrottlingDialog = scanThrottlingDialog(showDialog, currentStatus)
        )

        if (previousStatus.isEnabled != currentStatus.isEnabled && scanJob?.isActive == true) {
            restartScan()
        }
    }

    private fun scanThrottlingDialog(
        showDialog: Boolean,
        currentStatus: WifiScanThrottling
    ) = when {
        showDialog -> scanThrottlingUiMapper.mapDialog(currentStatus)
        currentStatus.isEnabled == false -> null
        else -> uiState.scanThrottlingDialog
    }

    private fun restartScan() {
        scanJob?.cancel()
        startScan("Restarting Wi-Fi scan")
    }
}
