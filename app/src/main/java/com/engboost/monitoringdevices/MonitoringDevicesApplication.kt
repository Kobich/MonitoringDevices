package com.engboost.monitoringdevices

import android.app.Application
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.DeviceMonitoringConfig
import com.engboost.monitoringdevices.monitoring.MonitoringSource

class MonitoringDevicesApplication : Application() {
    /** The single place to switch sources, e.g. `setOf(MonitoringSource.BLUETOOTH)` for Bluetooth only. */
    val monitoring: DeviceMonitoring by lazy {
        DeviceMonitoring(
            context = this,
            config = DeviceMonitoringConfig(
                enabledSources = setOf(MonitoringSource.WIFI, MonitoringSource.BLUETOOTH)
            )
        )
    }
}
