package com.engboost.monitoringdevices.scanner.wifi

/** Event of a Wi-Fi scan. */
sealed interface WifiScanEvent {
    /** The scanner has started; results follow as [Networks]. */
    data object Scanning : WifiScanEvent

    /** Networks found by the last scan. */
    data class Networks(
        val networks: List<ScannedWifiNetwork>
    ) : WifiScanEvent

    /** Terminal event: [permissions] must be granted to continue. */
    data class PermissionRequired(
        val permissions: Set<String>
    ) : WifiScanEvent

    /** Terminal event: scanning is impossible in the current device state. */
    data class Unavailable(
        val reason: WifiUnavailableReason
    ) : WifiScanEvent

    /** Terminal event: scanning failed with [message] and an optional [cause]. */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : WifiScanEvent
}

/** Reason why Wi-Fi scanning is unavailable. */
enum class WifiUnavailableReason {
    WIFI_DISABLED,
    LOCATION_DISABLED,

    /** Android rejected the scan request and there are no cached results. */
    SCAN_REJECTED
}
