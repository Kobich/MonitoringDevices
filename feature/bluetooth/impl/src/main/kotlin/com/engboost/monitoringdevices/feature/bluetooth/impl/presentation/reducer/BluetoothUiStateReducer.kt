package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.reducer

import com.engboost.monitoringdevices.feature.bluetooth.impl.domain.model.BluetoothScanResult
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.mapper.BluetoothDeviceUiMapper
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothSortMode
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothUiState
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.PermissionRequestUi

internal class BluetoothUiStateReducer(
    private val deviceUiMapper: BluetoothDeviceUiMapper
) {
    fun reduce(
        state: BluetoothUiState,
        result: BluetoothScanResult
    ): BluetoothUiState {
        return when (result) {
            BluetoothScanResult.Scanning -> state.copy(
                statusText = "Bluetooth scan running",
                isScanning = true,
                permissionRequest = null
            )

            is BluetoothScanResult.Devices -> {
                val devices = deviceUiMapper.map(
                    devices = result.devices,
                    sortMode = state.sortMode,
                    currentDevices = state.devices
                )
                state.copy(
                    statusText = "Bluetooth data received",
                    devices = devices,
                    selectedDevice = state.selectedDevice?.let { selectedDevice ->
                        devices.find { device -> device.address == selectedDevice.address }
                    },
                    isScanning = true,
                    permissionRequest = null
                )
            }

            is BluetoothScanResult.PermissionRequired -> state.copy(
                statusText = "Permissions required",
                isScanning = false,
                permissionRequest = result.permissions.toPermissionRequest(state.permissionRequest)
            )

            is BluetoothScanResult.Unavailable -> state.copy(
                statusText = result.reason,
                devices = emptyList(),
                selectedDevice = null,
                isScanning = false,
                permissionRequest = null
            )

            is BluetoothScanResult.Error -> state.copy(
                statusText = result.cause
                    ?.let { cause -> "${result.message}: ${cause.toDisplayMessage()}" }
                    ?: result.message,
                isScanning = false,
                permissionRequest = null
            )
        }
    }

    fun changeSortMode(
        state: BluetoothUiState,
        sortMode: BluetoothSortMode
    ): BluetoothUiState {
        return state.copy(
            sortMode = sortMode,
            devices = deviceUiMapper.sort(state.devices, sortMode)
        )
    }

    private fun Set<String>.toPermissionRequest(currentRequest: PermissionRequestUi?): PermissionRequestUi {
        return PermissionRequestUi(
            id = currentRequest.nextId(),
            permissions = toList()
        )
    }

    private fun PermissionRequestUi?.nextId(): Int = (this?.id ?: 0) + 1

    private fun Throwable.toDisplayMessage(): String {
        return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
    }
}
