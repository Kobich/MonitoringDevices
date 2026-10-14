package com.engboost.monitoringdevices

import android.app.Application
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.DeviceMonitoringConfig
import com.engboost.monitoringdevices.monitoring.MonitoringSource

class MonitoringDevicesApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // The single place to switch sources, e.g. setOf(MonitoringSource.BLUETOOTH) for Bluetooth only.
        DeviceMonitoring.initialize(
            context = this,
            config = DeviceMonitoringConfig(
                enabledSources = setOf(MonitoringSource.WIFI, MonitoringSource.BLUETOOTH)
            )
        )
    }
}
