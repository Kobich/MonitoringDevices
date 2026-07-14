package com.engboost.monitoringdevices.feature.wifi.impl.domain.model

import com.engboost.monitoringdevices.scanner.wifi.api.WifiNetwork

internal sealed interface WifiScanResult {
    data object Scanning : WifiScanResult

    data class Networks(
        val networks: List<WifiNetwork>
    ) : WifiScanResult

    data class PermissionRequired(
        val permissions: Set<String>
    ) : WifiScanResult

    data class Unavailable(
        val reason: String
    ) : WifiScanResult

    data class Error(
        val message: String,
        val cause: Throwable?
    ) : WifiScanResult
}
