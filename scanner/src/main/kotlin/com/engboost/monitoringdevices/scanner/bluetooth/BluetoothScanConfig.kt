package com.engboost.monitoringdevices.scanner.bluetooth

/**
 * Advertisements arrive many times per second, so they are accumulated and published as snapshots.
 *
 * @property snapshotIntervalMillis a snapshot is sent at most this often and only if something changed.
 * @property ageRefreshIntervalMillis how often device ages are refreshed and expired devices are removed.
 * @property retentionMillis a device not seen for this long is removed from the list.
 * @property rssiSmoothingFactor exponential smoothing factor in `(0, 1]`; `1` disables smoothing.
 * @property minRssiChangeDbm smoothed RSSI changes smaller than this are not reported.
 */
data class BluetoothScanConfig(
    val snapshotIntervalMillis: Long = 750L,
    val ageRefreshIntervalMillis: Long = 5_000L,
    val staleAfterMillis: Long = 30_000L,
    val retentionMillis: Long = 120_000L,
    val rssiSmoothingFactor: Double = 0.25,
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
