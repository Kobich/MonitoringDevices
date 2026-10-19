package com.engboost.monitoringdevices.scanner.wifi

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface WifiScannerApi {
    val requiredPermissions: Set<String>

    /** Set in Developer options; while enabled, [WifiScanConfig.throttledRefreshIntervalMillis] is used. */
    val isScanThrottlingEnabled: Boolean

    fun missingPermissions(): Set<String>

    /**
     * Scans while the returned cold flow is collected.
     *
     * The flow completes by itself only after a terminal event.
     */
    fun scan(): Flow<WifiScanEvent>
}

fun WifiScannerApi(
    context: Context,
    config: WifiScanConfig = WifiScanConfig()
): WifiScannerApi = WifiScannerImpl(context, config)
