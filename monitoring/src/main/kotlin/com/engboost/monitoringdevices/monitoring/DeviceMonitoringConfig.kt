package com.engboost.monitoringdevices.monitoring

import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothMonitoringConfig
import com.engboost.monitoringdevices.monitoring.wifi.WifiMonitoringConfig

/**
 * Configuration of [DeviceMonitoring].
 *
 * @property enabledSources sources that can be scanned; scanning another source reports
 * `Unavailable` with the `SOURCE_DISABLED` reason.
 * @property wifi parameters of Wi-Fi scanning.
 * @property bluetooth parameters of Bluetooth scanning.
 */
data class DeviceMonitoringConfig(
    val enabledSources: Set<MonitoringSource> = MonitoringSource.entries.toSet(),
    val wifi: WifiMonitoringConfig = WifiMonitoringConfig(),
    val bluetooth: BluetoothMonitoringConfig = BluetoothMonitoringConfig()
)
