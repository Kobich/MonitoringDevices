package com.engboost.monitoringdevices.monitoring

import androidx.annotation.DrawableRes

data class DeviceMonitoringConfig(
    val enabledSources: Set<MonitoringSource> = MonitoringSource.entries.toSet(),
    val wifi: WifiMonitoringConfig = WifiMonitoringConfig(),
    val ble: BleMonitoringConfig = BleMonitoringConfig(),
    val notification: MonitoringNotificationConfig = MonitoringNotificationConfig()
)

data class WifiMonitoringConfig(
    /** Interval between scan requests while Android Wi-Fi scan throttling is enabled. */
    val throttledRefreshIntervalMillis: Long = 30_000L,
    /** Interval between scan requests while Wi-Fi scan throttling is disabled. */
    val unthrottledRefreshIntervalMillis: Long = 5_000L
) {
    init {
        require(throttledRefreshIntervalMillis > 0) { "throttledRefreshIntervalMillis must be positive" }
        require(unthrottledRefreshIntervalMillis > 0) { "unthrottledRefreshIntervalMillis must be positive" }
    }
}

data class BleMonitoringConfig(
    /** Minimal interval between device list snapshots. */
    val snapshotIntervalMillis: Long = 750L,
    /** How often device ages are refreshed and expired devices are removed. */
    val ageRefreshIntervalMillis: Long = 5_000L,
    /** A device not seen for this long is reported with `isStale = true`. */
    val staleAfterMillis: Long = 30_000L,
    /** A device not seen for this long is removed from the list. */
    val retentionMillis: Long = 120_000L,
    /** Exponential RSSI smoothing factor in (0, 1]; 1 disables smoothing. */
    val rssiSmoothingFactor: Double = 0.25,
    /** Smoothed RSSI changes smaller than this are not reported. */
    val minRssiChangeDbm: Int = 3
) {
    init {
        require(snapshotIntervalMillis > 0) { "snapshotIntervalMillis must be positive" }
        require(ageRefreshIntervalMillis > 0) { "ageRefreshIntervalMillis must be positive" }
        require(staleAfterMillis in 1..retentionMillis) { "staleAfterMillis must be in 1..retentionMillis" }
        require(rssiSmoothingFactor > 0.0 && rssiSmoothingFactor <= 1.0) {
            "rssiSmoothingFactor must be in (0, 1]"
        }
        require(minRssiChangeDbm >= 0) { "minRssiChangeDbm must not be negative" }
    }
}

/**
 * Notification texts are library string resources prefixed with `device_monitoring_`
 * and can be overridden in the host app.
 */
data class MonitoringNotificationConfig(
    /** Small icon of the foreground notification; `null` uses the library icon. */
    @DrawableRes val smallIconRes: Int? = null
)
