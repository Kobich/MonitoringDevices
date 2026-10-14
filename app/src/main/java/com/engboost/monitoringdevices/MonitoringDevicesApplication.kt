package com.engboost.monitoringdevices

import android.app.Application
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.DeviceMonitoringConfig
import com.engboost.monitoringdevices.monitoring.MonitoringSource

class MonitoringDevicesApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Single place to switch sources: e.g. setOf(MonitoringSource.BLE) for BLE only.
        DeviceMonitoring.initialize(
            context = this,
            config = DeviceMonitoringConfig(
                enabledSources = setOf(MonitoringSource.WIFI, MonitoringSource.BLE)
            )
        )
    }
}
