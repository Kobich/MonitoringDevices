package com.engboost.monitoringdevices.scanner.ui.bluetooth

import com.engboost.monitoringdevices.scanner.bluetooth.BluetoothScanEvent
import com.engboost.monitoringdevices.scanner.ui.common.ScanStatus
import com.engboost.monitoringdevices.scanner.ui.common.errorText
import com.engboost.monitoringdevices.scanner.ui.common.nextPermissionRequest

private val deviceUiMapper = BluetoothDeviceUiMapper()

/** Applies a scanner [event] to the screen state. */
internal fun BluetoothUiState.reduce(event: BluetoothScanEvent): BluetoothUiState {
    return when (event) {
        BluetoothScanEvent.Scanning -> copy(status = ScanStatus.Scanning)

        is BluetoothScanEvent.Devices -> {
            val devices = deviceUiMapper.map(
                devices = event.devices,
                sortMode = sortMode,
                currentDevices = this.devices
            )
            copy(
                status = ScanStatus.Scanning,
                devices = devices,
                selectedDevice = selectedDevice?.let { selected ->
                    devices.find { device -> device.address == selected.address }
                }
            )
        }

        is BluetoothScanEvent.PermissionRequired -> withPermissionRequest(event.permissions)

        is BluetoothScanEvent.Unavailable -> copy(
            status = ScanStatus.Unavailable(event.reason),
            devices = emptyList(),
            selectedDevice = null
        )

        is BluetoothScanEvent.Error -> copy(status = ScanStatus.Failed(errorText(event.message, event.cause)))
    }
}

internal fun BluetoothUiState.withPermissionRequest(permissions: Set<String>): BluetoothUiState {
    return copy(
        status = ScanStatus.PermissionsRequired,
        permissionRequest = nextPermissionRequest(permissionRequest, permissions)
    )
}

internal fun BluetoothUiState.withSortMode(sortMode: BluetoothSortMode): BluetoothUiState {
    return copy(
        sortMode = sortMode,
        devices = deviceUiMapper.sort(devices, sortMode)
    )
}
