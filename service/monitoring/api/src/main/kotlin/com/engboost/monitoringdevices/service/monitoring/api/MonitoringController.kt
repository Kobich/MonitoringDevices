package com.engboost.monitoringdevices.service.monitoring.api

import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MonitoringController {
    val state: StateFlow<MonitoringState>
    val wifiEvents: Flow<WifiScanEvent>
    val bluetoothEvents: Flow<BluetoothScanEvent>

    fun startMonitoring(type: MonitoringType): Set<String>
    fun stopMonitoring(type: MonitoringType)
    fun stopAllMonitoring()
}

enum class MonitoringType {
    WIFI,
    BLUETOOTH
}

data class MonitoringState(
    val activeTypes: Set<MonitoringType>,
    val wifiNetworkCount: Int,
    val bluetoothDeviceCount: Int
)
