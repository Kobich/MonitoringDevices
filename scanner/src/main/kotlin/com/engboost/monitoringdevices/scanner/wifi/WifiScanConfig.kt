package com.engboost.monitoringdevices.scanner.wifi

/**
 * Android allows four scans in two minutes while scan throttling is enabled, so there are two intervals.
 * The throttling status is re-read before every wait.
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
