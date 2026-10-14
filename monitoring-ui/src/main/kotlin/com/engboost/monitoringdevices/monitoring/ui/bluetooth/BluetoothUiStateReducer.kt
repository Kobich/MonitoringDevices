package com.engboost.monitoringdevices.monitoring.ui.bluetooth

import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothScanEvent
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothUnavailableReason
import com.engboost.monitoringdevices.monitoring.ui.common.errorText
import com.engboost.monitoringdevices.monitoring.ui.common.nextPermissionRequest

private val deviceUiMapper = BluetoothDeviceUiMapper()

/** Applies a scanner [event] to the screen state. */
internal fun BluetoothUiState.reduce(event: BluetoothScanEvent): BluetoothUiState {
    return when (event) {
        BluetoothScanEvent.Scanning -> copy(
            statusText = "Bluetooth scan running",
            isScanning = true,
            permissionRequest = null
        )

        is BluetoothScanEvent.Devices -> {
            val devices = deviceUiMapper.map(
                devices = event.devices,
                sortMode = sortMode,
                currentDevices = this.devices
            )
            copy(
                statusText = "Bluetooth data received",
                devices = devices,
                selectedDevice = selectedDevice?.let { selected ->
                    devices.find { device -> device.address == selected.address }
                },
                isScanning = true,
                permissionRequest = null
            )
        }

        is BluetoothScanEvent.PermissionRequired -> withPermissionRequest(event.permissions)

        is BluetoothScanEvent.Unavailable -> copy(
            statusText = event.reason.toDisplayText(),
            devices = emptyList(),
            selectedDevice = null,
            isScanning = false,
            permissionRequest = null
        )

        is BluetoothScanEvent.Error -> stopped(errorText(event.message, event.cause))
    }
}

internal fun BluetoothUiState.withPermissionRequest(permissions: Set<String>): BluetoothUiState {
    return copy(
        statusText = "Permissions required",
        isScanning = false,
        permissionRequest = nextPermissionRequest(permissionRequest, permissions)
    )
}

internal fun BluetoothUiState.withSortMode(sortMode: BluetoothSortMode): BluetoothUiState {
    return copy(
        sortMode = sortMode,
        devices = deviceUiMapper.sort(devices, sortMode)
    )
}

internal fun BluetoothUiState.stopped(statusText: String): BluetoothUiState {
    return copy(
        statusText = statusText,
        isScanning = false,
        permissionRequest = null
    )
}

private fun BluetoothUnavailableReason.toDisplayText(): String {
    return when (this) {
        BluetoothUnavailableReason.ADAPTER_UNAVAILABLE -> "Bluetooth adapter is not available"
        BluetoothUnavailableReason.BLUETOOTH_DISABLED -> "Bluetooth is disabled"
        BluetoothUnavailableReason.LE_SCANNER_UNAVAILABLE -> "Bluetooth LE scanner is not available"
    }
}
