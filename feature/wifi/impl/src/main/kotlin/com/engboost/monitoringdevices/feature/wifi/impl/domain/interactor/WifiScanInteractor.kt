package com.engboost.monitoringdevices.feature.wifi.impl.domain.interactor

import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
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

internal class WifiScanInteractor(
    private val scanner: WifiScanner,
    private val monitoringController: MonitoringController
) {
    val isScanThrottlingEnabled: Boolean
        get() = scanner.isScanThrottlingEnabled
    val isMonitoringActive: Boolean
        get() = MonitoringType.WIFI in monitoringController.state.value.activeTypes

    fun scan(): Flow<WifiScanEvent> {
        return flow {
            val missingPermissions = monitoringController.startMonitoring(MonitoringType.WIFI)
            if (missingPermissions.isNotEmpty()) {
                emit(WifiScanEvent.PermissionRequired(missingPermissions))
            } else {
                emitAll(backgroundEvents())
            }
        }
    }

    fun stopMonitoring() {
        monitoringController.stopMonitoring(MonitoringType.WIFI)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun backgroundEvents(): Flow<WifiScanEvent> {
        return monitoringController.state
            .flatMapLatest { state ->
                if (MonitoringType.WIFI in state.activeTypes) {
                    monitoringController.wifiEvents.map { event -> event as WifiScanEvent? }
                } else {
                    flowOf(null)
                }
            }
            .takeWhile { event -> event != null }
            .filterNotNull()
            .transformWhile { event ->
                emit(event)
                event == WifiScanEvent.Scanning || event is WifiScanEvent.Networks
            }
    }
}
