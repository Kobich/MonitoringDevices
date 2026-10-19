package com.engboost.monitoringdevices.scanner.wifi

sealed interface WifiScanEvent {
    data object Scanning : WifiScanEvent

    data class Networks(
        val networks: List<ScannedWifiNetwork>
    ) : WifiScanEvent

    /** Terminal event. */
    data class PermissionRequired(
        val permissions: Set<String>
    ) : WifiScanEvent

    /** Terminal event. */
    data class Unavailable(
        val reason: WifiUnavailableReason
    ) : WifiScanEvent

    /** Terminal event. */
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
