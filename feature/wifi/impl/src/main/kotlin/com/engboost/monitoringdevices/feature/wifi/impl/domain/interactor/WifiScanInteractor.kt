package com.engboost.monitoringdevices.feature.wifi.impl.domain.interactor

import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanConfig
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import kotlinx.coroutines.flow.Flow

internal class WifiScanInteractor(
    private val scanner: WifiScanner
) {
    val isScanThrottlingEnabled: Boolean
        get() = scanner.isScanThrottlingEnabled

    fun scan(): Flow<WifiScanEvent> {
        return scanner.scan(WifiScanConfig(refreshIntervalMillis = refreshIntervalMillis()))
    }

    private fun refreshIntervalMillis(): Long {
        return if (!isScanThrottlingEnabled) {
            UNTHROTTLED_REFRESH_INTERVAL_MILLIS
        } else {
            THROTTLED_REFRESH_INTERVAL_MILLIS
        }
    }

    private companion object {
        const val THROTTLED_REFRESH_INTERVAL_MILLIS = 30_000L
        const val UNTHROTTLED_REFRESH_INTERVAL_MILLIS = 5_000L
    }
}
