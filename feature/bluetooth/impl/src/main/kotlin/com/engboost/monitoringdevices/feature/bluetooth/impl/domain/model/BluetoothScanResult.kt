package com.engboost.monitoringdevices.feature.bluetooth.impl.domain.model

import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceInfo

internal sealed interface BluetoothScanResult {
    data object Scanning : BluetoothScanResult

    data class Devices(
        val devices: List<BluetoothDeviceInfo>
    ) : BluetoothScanResult

    data class PermissionRequired(
        val permissions: Set<String>
    ) : BluetoothScanResult

    data class Unavailable(
        val reason: String
    ) : BluetoothScanResult

    data class Error(
        val message: String,
        val cause: Throwable?
    ) : BluetoothScanResult
}
