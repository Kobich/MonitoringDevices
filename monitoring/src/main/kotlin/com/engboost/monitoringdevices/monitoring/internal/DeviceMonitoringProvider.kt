package com.engboost.monitoringdevices.monitoring.internal

import android.content.Context
import com.engboost.monitoringdevices.monitoring.DeviceMonitoringConfig
import com.engboost.monitoringdevices.monitoring.internal.ble.AndroidBleScanner
import com.engboost.monitoringdevices.monitoring.internal.wifi.AndroidWifiScanner

internal object DeviceMonitoringProvider {
    @Volatile
    private var instance: DefaultDeviceMonitoring? = null

    fun initialize(context: Context, config: DeviceMonitoringConfig): DefaultDeviceMonitoring {
        synchronized(this) {
            instance?.let { current ->
                check(current.config == config) {
                    "DeviceMonitoring is already initialized with a different config"
                }
                return current
            }
            return create(context, config).also { instance = it }
        }
    }

    fun get(context: Context): DefaultDeviceMonitoring {
        instance?.let { return it }
        synchronized(this) {
            return instance ?: create(context, DeviceMonitoringConfig()).also { instance = it }
        }
    }

    private fun create(context: Context, config: DeviceMonitoringConfig): DefaultDeviceMonitoring {
        val appContext = context.applicationContext
        return DefaultDeviceMonitoring(
            context = appContext,
            config = config,
            wifiScanner = AndroidWifiScanner(appContext),
            bleScanner = AndroidBleScanner(appContext, config.ble)
        )
    }
}
