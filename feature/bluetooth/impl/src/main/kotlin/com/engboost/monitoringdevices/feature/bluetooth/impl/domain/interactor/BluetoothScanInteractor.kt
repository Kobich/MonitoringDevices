package com.engboost.monitoringdevices.feature.bluetooth.impl.domain.interactor

import com.engboost.monitoringdevices.feature.bluetooth.impl.domain.model.BluetoothScanResult
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanEvent
import com.engboost.monitoringdevices.service.monitoring.api.MonitoringController
import com.engboost.monitoringdevices.service.monitoring.api.MonitoringType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile

internal class BluetoothScanInteractor(
    private val monitoringController: MonitoringController
) {
    val isMonitoringActive: Boolean
        get() = MonitoringType.BLUETOOTH in monitoringController.state.value.activeTypes

    fun scan(): Flow<BluetoothScanResult> {
        return flow {
            val missingPermissions = monitoringController.startMonitoring(MonitoringType.BLUETOOTH)
            if (missingPermissions.isNotEmpty()) {
                emit(BluetoothScanEvent.PermissionRequired(missingPermissions))
            } else {
                emitAll(backgroundEvents())
            }
        }.map { event -> event.toResult() }
    }

    fun stopMonitoring() {
        monitoringController.stopMonitoring(MonitoringType.BLUETOOTH)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun backgroundEvents(): Flow<BluetoothScanEvent> {
        return monitoringController.state
            .flatMapLatest { state ->
                if (MonitoringType.BLUETOOTH in state.activeTypes) {
                    monitoringController.bluetoothEvents.map { event -> event as BluetoothScanEvent? }
                } else {
                    flowOf(null)
                }
            }
            .takeWhile { event -> event != null }
            .filterNotNull()
            .transformWhile { event ->
                emit(event)
                event == BluetoothScanEvent.Scanning || event is BluetoothScanEvent.Devices
            }
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
