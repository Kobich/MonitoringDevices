package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.feature.bluetooth.impl.domain.interactor.BluetoothScanInteractor
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothDeviceUi
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothSortMode
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothUiState
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.reducer.BluetoothUiStateReducer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

internal class BluetoothViewModel(
    private val scanInteractor: BluetoothScanInteractor,
    private val uiStateReducer: BluetoothUiStateReducer
) : ViewModel() {
    private var scanJob: Job? = null

    var uiState by mutableStateOf(
        BluetoothUiState(
            statusText = "Stopped",
            devices = emptyList(),
            selectedDevice = null,
            sortMode = BluetoothSortMode.STABLE,
            isScanning = false,
            permissionRequest = null
        )
    )
        private set

    init {
        if (scanInteractor.isMonitoringActive) {
            onStartClick()
        }
    }

    fun onStartClick() {
        if (scanJob?.isActive == true) return

        uiState = uiState.copy(
            statusText = "Starting Bluetooth scan",
            isScanning = true,
            permissionRequest = null
        )

        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            scanInteractor.scan()
                .catch { error ->
                    uiState = uiState.copy(
                        statusText = "Bluetooth scan error: ${error.toDisplayMessage()}",
                        isScanning = false,
                        permissionRequest = null
                    )
                }
                .onCompletion {
                    if (scanJob === job) {
                        scanJob = null
                        if (uiState.isScanning) {
                            uiState = uiState.copy(
                                statusText = "Stopped",
                                isScanning = false,
                                permissionRequest = null
                            )
                        }
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
        scanInteractor.stopMonitoring()
        scanJob?.cancel()
        scanJob = null
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

    fun onDeviceClick(device: BluetoothDeviceUi) {
        uiState = uiState.copy(selectedDevice = device)
    }

    fun onDeviceDetailsDismiss() {
        uiState = uiState.copy(selectedDevice = null)
    }

    fun onSortModeChange(sortMode: BluetoothSortMode) {
        uiState = uiStateReducer.changeSortMode(uiState, sortMode)
    }

    private fun Throwable.toDisplayMessage(): String {
        return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
    }
}
