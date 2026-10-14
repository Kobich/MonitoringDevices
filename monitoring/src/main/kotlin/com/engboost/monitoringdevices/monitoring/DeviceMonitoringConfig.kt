package com.engboost.monitoringdevices.monitoring

import androidx.annotation.DrawableRes
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothMonitoringConfig
import com.engboost.monitoringdevices.monitoring.wifi.WifiMonitoringConfig

/**
 * Configuration of [DeviceMonitoring].
 *
 * @property enabledSources sources that can be started; the other ones return
 * [StartResult.SourceDisabled].
 * @property wifi parameters of Wi-Fi monitoring.
 * @property bluetooth parameters of Bluetooth monitoring.
 * @property notification appearance of the foreground service notification.
 */
data class DeviceMonitoringConfig(
    val enabledSources: Set<MonitoringSource> = MonitoringSource.entries.toSet(),
    val wifi: WifiMonitoringConfig = WifiMonitoringConfig(),
    val bluetooth: BluetoothMonitoringConfig = BluetoothMonitoringConfig(),
    val notification: MonitoringNotificationConfig = MonitoringNotificationConfig()
)

/**
 * Appearance of the foreground service notification.
 *
 * Notification texts are string resources prefixed with `device_monitoring_`
 * and can be overridden in the host app.
 *
 * @property smallIconRes small icon of the notification; `null` uses the library icon.
 */
data class MonitoringNotificationConfig(
    @DrawableRes val smallIconRes: Int? = null
)
