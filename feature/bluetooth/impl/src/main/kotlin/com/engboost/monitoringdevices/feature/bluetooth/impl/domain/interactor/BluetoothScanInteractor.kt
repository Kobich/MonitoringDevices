package com.engboost.monitoringdevices.feature.bluetooth.impl.domain.interactor

import com.engboost.monitoringdevices.feature.bluetooth.impl.domain.model.BluetoothScanResult
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanEvent
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class BluetoothScanInteractor(
    private val scanner: BluetoothScanner
) {
    val requiredPermissions: Set<String> = scanner.requiredPermissions

    fun scan(): Flow<BluetoothScanResult> {
        return scanner.scan().map { event -> event.toResult() }
    }

    private fun BluetoothScanEvent.toResult(): BluetoothScanResult {
        return when (this) {
            BluetoothScanEvent.Scanning -> BluetoothScanResult.Scanning
            is BluetoothScanEvent.Devices -> BluetoothScanResult.Devices(devices)
            is BluetoothScanEvent.PermissionRequired -> BluetoothScanResult.PermissionRequired(permissions)
            is BluetoothScanEvent.Unavailable -> BluetoothScanResult.Unavailable(reason)
            is BluetoothScanEvent.Error -> BluetoothScanResult.Error(
                message = message,
                cause = cause
            )
        }
    }
}
