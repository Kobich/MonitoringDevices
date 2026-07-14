package com.engboost.monitoringdevices.feature.wifi.impl.domain.interactor

import com.engboost.monitoringdevices.feature.wifi.impl.domain.model.WifiScanResult
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanConfig
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class WifiScanInteractor(
    private val scanner: WifiScanner
) {
    val requiredPermissions: Set<String> = scanner.requiredPermissions

    fun scan(): Flow<WifiScanResult> {
        return scanner.scan(WifiScanConfig(refreshIntervalMillis = WIFI_REFRESH_INTERVAL_MILLIS))
            .map { event -> event.toResult() }
    }

    private fun WifiScanEvent.toResult(): WifiScanResult {
        return when (this) {
            WifiScanEvent.Scanning -> WifiScanResult.Scanning
            is WifiScanEvent.Networks -> WifiScanResult.Networks(networks)
            is WifiScanEvent.PermissionRequired -> WifiScanResult.PermissionRequired(permissions)
            is WifiScanEvent.Unavailable -> WifiScanResult.Unavailable(reason)
            is WifiScanEvent.Error -> WifiScanResult.Error(
                message = message,
                cause = cause
            )
        }
    }

    private companion object {
        const val WIFI_REFRESH_INTERVAL_MILLIS = 30_000L
    }
}
