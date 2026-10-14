package com.engboost.monitoringdevices.monitoring.wifi

sealed interface WifiScanEvent {
    data object Scanning : WifiScanEvent

    data class Networks(
        val networks: List<WifiNetwork>
    ) : WifiScanEvent

    data class PermissionRequired(
        val permissions: Set<String>
    ) : WifiScanEvent

    data class Unavailable(
        val reason: WifiUnavailableReason
    ) : WifiScanEvent

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : WifiScanEvent
}

enum class WifiUnavailableReason {
    WIFI_DISABLED,
    LOCATION_DISABLED,
    /** Android rejected the scan request and there are no cached results. */
    SCAN_REJECTED
}

data class WifiNetwork(
    val ssid: String?,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)
