package com.engboost.monitoringdevices.monitoring.internal.wifi

import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import kotlinx.coroutines.flow.Flow

internal interface WifiScanner {
    val requiredPermissions: Set<String>
    val isScanThrottlingEnabled: Boolean

    /** Requests a scan, then waits [refreshIntervalMillis] (read before every wait) and repeats. */
    fun scan(refreshIntervalMillis: () -> Long): Flow<WifiScanEvent>
}
