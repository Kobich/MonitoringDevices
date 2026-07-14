package com.engboost.monitoringdevices.feature.wifi.impl.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.feature.wifi.impl.domain.interactor.WifiScanInteractor
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiNetworkUi
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiUiState
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.reducer.WifiUiStateReducer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

internal class WifiViewModel(
    private val scanInteractor: WifiScanInteractor,
    private val uiStateReducer: WifiUiStateReducer
) : ViewModel() {
    private var scanJob: Job? = null

    var uiState by mutableStateOf(
        WifiUiState(
            statusText = "Stopped",
            permissionText = scanInteractor.requiredPermissions.joinToString(),
            networks = emptyList(),
            selectedNetwork = null,
            isScanning = false,
            permissionRequest = null
        )
    )
        private set

    fun onStartClick() {
        if (scanJob?.isActive == true) return

        uiState = uiState.copy(
            statusText = "Starting Wi-Fi scan",
            isScanning = true,
            permissionRequest = null
        )

        scanJob = viewModelScope.launch {
            scanInteractor.scan()
                .catch {
                    uiState = uiState.copy(
                        statusText = "Wi-Fi scan error",
                        isScanning = false,
                        permissionRequest = null
                    )
                }
                .onCompletion { scanJob = null }
                .collect { result ->
                    uiState = uiStateReducer.reduce(uiState, result)
                }
        }
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
}
