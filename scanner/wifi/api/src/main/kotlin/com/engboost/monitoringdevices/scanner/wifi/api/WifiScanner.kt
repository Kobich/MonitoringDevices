package com.engboost.monitoringdevices.scanner.wifi.api

import kotlinx.coroutines.flow.Flow

interface WifiScanner {
    val requiredPermissions: Set<String>
    val scanThrottlingStatus: WifiScanThrottlingStatus

    fun scan(config: WifiScanConfig = WifiScanConfig()): Flow<WifiScanEvent>
}

data class WifiScanThrottlingStatus(
    val isEnabled: Boolean?
)

data class WifiScanConfig(
    val mode: WifiScanMode = WifiScanMode.ACTIVE,
    val refreshIntervalMillis: Long? = null
) {
    init {
        require(refreshIntervalMillis == null || refreshIntervalMillis > 0) {
            "refreshIntervalMillis must be positive"
        }
    }
}

enum class WifiScanMode {
    ACTIVE,
    CACHED_ONLY
}

sealed interface WifiScanEvent {
    data object Scanning : WifiScanEvent

    data class Networks(
        val networks: List<WifiNetwork>
    ) : WifiScanEvent

    data class PermissionRequired(
        val permissions: Set<String>
    ) : WifiScanEvent

    data class Unavailable(
        val reason: String
    ) : WifiScanEvent

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : WifiScanEvent
}

data class WifiNetwork(
    val ssid: String?,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)
