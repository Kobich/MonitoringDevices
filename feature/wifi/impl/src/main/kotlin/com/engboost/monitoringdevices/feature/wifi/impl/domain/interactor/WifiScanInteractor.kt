package com.engboost.monitoringdevices.feature.wifi.impl.domain.interactor

import com.engboost.monitoringdevices.feature.wifi.impl.domain.model.WifiScanResult
import com.engboost.monitoringdevices.feature.wifi.impl.domain.model.WifiScanThrottling
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanConfig
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanThrottlingStatus
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class WifiScanInteractor(
    private val scanner: WifiScanner
) {
    val requiredPermissions: Set<String> = scanner.requiredPermissions
    val scanThrottling: WifiScanThrottling
        get() = scanner.scanThrottlingStatus.toDomain()

    fun scan(): Flow<WifiScanResult> {
        return scanner.scan(WifiScanConfig(refreshIntervalMillis = refreshIntervalMillis()))
            .map { event -> event.toResult() }
    }

    private fun refreshIntervalMillis(): Long {
        return if (scanThrottling.isEnabled == false) {
            UNTHROTTLED_REFRESH_INTERVAL_MILLIS
        } else {
            THROTTLED_REFRESH_INTERVAL_MILLIS
        }
    }

    private fun WifiScanThrottlingStatus.toDomain(): WifiScanThrottling {
        return WifiScanThrottling(isEnabled = isEnabled)
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
        const val THROTTLED_REFRESH_INTERVAL_MILLIS = 30_000L
        const val UNTHROTTLED_REFRESH_INTERVAL_MILLIS = 5_000L
    }
}
