package com.engboost.monitoringdevices.monitoring.ui.ble

import com.engboost.monitoringdevices.monitoring.ble.BleScanEvent
import com.engboost.monitoringdevices.monitoring.ble.BleUnavailableReason

internal class BleUiStateReducer(
    private val deviceUiMapper: BleDeviceUiMapper
) {
    fun reduce(
        state: BleUiState,
        event: BleScanEvent
    ): BleUiState {
        return when (event) {
            BleScanEvent.Scanning -> state.copy(
                statusText = "BLE scan running",
                isScanning = true,
                permissionRequest = null
            )

            is BleScanEvent.Devices -> {
                val devices = deviceUiMapper.map(
                    devices = event.devices,
                    sortMode = state.sortMode,
                    currentDevices = state.devices
                )
                state.copy(
                    statusText = "BLE data received",
                    devices = devices,
                    selectedDevice = state.selectedDevice?.let { selectedDevice ->
                        devices.find { device -> device.address == selectedDevice.address }
                    },
                    isScanning = true,
                    permissionRequest = null
                )
            }

            is BleScanEvent.PermissionRequired -> withPermissionRequest(state, event.permissions)

            is BleScanEvent.Unavailable -> state.copy(
                statusText = event.reason.toDisplayText(),
                devices = emptyList(),
                selectedDevice = null,
                isScanning = false,
                permissionRequest = null
            )

            is BleScanEvent.Error -> state.copy(
                statusText = event.cause
                    ?.let { cause -> "${event.message}: ${cause.toDisplayMessage()}" }
                    ?: event.message,
                isScanning = false,
                permissionRequest = null
            )
        }
    }

    fun withPermissionRequest(
        state: BleUiState,
        permissions: Set<String>
    ): BleUiState {
        return state.copy(
            statusText = "Permissions required",
            isScanning = false,
            permissionRequest = PermissionRequestUi(
                id = (state.permissionRequest?.id ?: 0) + 1,
                permissions = permissions.toList()
            )
        )
    }

    fun changeSortMode(
        state: BleUiState,
        sortMode: BleSortMode
    ): BleUiState {
        return state.copy(
            sortMode = sortMode,
            devices = deviceUiMapper.sort(state.devices, sortMode)
        )
    }

    private fun BleUnavailableReason.toDisplayText(): String {
        return when (this) {
            BleUnavailableReason.ADAPTER_UNAVAILABLE -> "Bluetooth adapter is not available"
            BleUnavailableReason.BLUETOOTH_DISABLED -> "Bluetooth is disabled"
            BleUnavailableReason.LE_SCANNER_UNAVAILABLE -> "Bluetooth LE scanner is not available"
        }
    }
}

internal fun Throwable.toDisplayMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}
