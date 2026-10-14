package com.engboost.monitoringdevices.scanner.wifi

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** Scanner of nearby Wi-Fi networks. */
interface WifiScanner {
    /** Runtime permissions required to scan. */
    val requiredPermissions: Set<String>

    /**
     * Whether Android Wi-Fi scan throttling is enabled in Developer options.
     *
     * While it is enabled, Android allows only a few scans per two minutes and
     * [WifiScanConfig.throttledRefreshIntervalMillis] is used.
     */
    val isScanThrottlingEnabled: Boolean

    /** Returns permissions from [requiredPermissions] that are not granted yet. */
    fun missingPermissions(): Set<String>

    /**
     * Returns a cold flow that scans while it is collected.
     *
     * The flow completes after a terminal event: [WifiScanEvent.PermissionRequired],
     * [WifiScanEvent.Unavailable] or [WifiScanEvent.Error].
     */
    fun scan(): Flow<WifiScanEvent>
}

/** Creates a [WifiScanner] backed by `WifiManager`. */
fun WifiScanner(
    context: Context,
    config: WifiScanConfig = WifiScanConfig()
): WifiScanner = AndroidWifiScanner(context, config)
