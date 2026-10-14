package com.engboost.monitoringdevices.scanner.wifi

/**
 * Parameters of Wi-Fi scanning.
 *
 * Android limits apps to four scans in two minutes while scan throttling is enabled,
 * so the interval depends on [WifiScanner.isScanThrottlingEnabled], which is re-read before every wait.
 *
 * @property throttledRefreshIntervalMillis interval between scan requests while scan throttling is enabled.
 * @property unthrottledRefreshIntervalMillis interval between scan requests while scan throttling is disabled.
 */
data class WifiScanConfig(
    val throttledRefreshIntervalMillis: Long = 30_000L,
    val unthrottledRefreshIntervalMillis: Long = 5_000L
) {
    init {
        require(throttledRefreshIntervalMillis > 0) { "throttledRefreshIntervalMillis must be positive" }
        require(unthrottledRefreshIntervalMillis > 0) { "unthrottledRefreshIntervalMillis must be positive" }
    }

    internal fun refreshIntervalMillis(isScanThrottlingEnabled: Boolean): Long {
        return if (isScanThrottlingEnabled) throttledRefreshIntervalMillis else unthrottledRefreshIntervalMillis
    }
}
