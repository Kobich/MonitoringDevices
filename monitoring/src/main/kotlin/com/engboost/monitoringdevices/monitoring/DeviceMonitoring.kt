package com.engboost.monitoringdevices.monitoring

import android.content.Context
import com.engboost.monitoringdevices.monitoring.ble.BleScanEvent
import com.engboost.monitoringdevices.monitoring.internal.DeviceMonitoringProvider
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry point of the library. Wi-Fi and BLE monitoring run in one foreground service,
 * each source is started and stopped independently.
 *
 * Obtain the process-wide instance with [initialize] (usually in `Application.onCreate`)
 * or [get]. The interface can be bound into any DI container or replaced with a fake.
 */
interface DeviceMonitoring {
    val config: DeviceMonitoringConfig
    val state: StateFlow<MonitoringState>

    /** Current Android Wi-Fi scan throttling status (Developer options). */
    val isWifiScanThrottlingEnabled: Boolean

    /** Runtime permissions the source needs, including notification permission on Android 13+. */
    fun requiredPermissions(source: MonitoringSource): Set<String>

    fun missingPermissions(source: MonitoringSource): Set<String>

    fun start(source: MonitoringSource): StartResult

    fun stop(source: MonitoringSource)

    fun stopAll()

    /**
     * Events of the current Wi-Fi session. The last event is replayed to new collectors.
     * Completes when Wi-Fi monitoring stops or after a terminal event
     * ([WifiScanEvent.PermissionRequired], [WifiScanEvent.Unavailable], [WifiScanEvent.Error]).
     */
    fun wifiEvents(): Flow<WifiScanEvent>

    /** Events of the current BLE session. Same completion rules as [wifiEvents]. */
    fun bleEvents(): Flow<BleScanEvent>

    companion object {
        /**
         * Creates the process-wide instance with [config]. Call it before the first [get],
         * typically in `Application.onCreate`, so a service restarted by Android uses the same config.
         */
        fun initialize(
            context: Context,
            config: DeviceMonitoringConfig = DeviceMonitoringConfig()
        ): DeviceMonitoring = DeviceMonitoringProvider.initialize(context, config)

        /** Returns the process-wide instance, creating it with the default config if needed. */
        fun get(context: Context): DeviceMonitoring = DeviceMonitoringProvider.get(context)
    }
}

enum class MonitoringSource {
    WIFI,
    BLE
}

data class MonitoringState(
    val activeSources: Set<MonitoringSource>,
    val wifiNetworkCount: Int,
    val bleDeviceCount: Int
)

sealed interface StartResult {
    data object Started : StartResult

    data class PermissionsRequired(
        val permissions: Set<String>
    ) : StartResult

    /** The source is not listed in [DeviceMonitoringConfig.enabledSources]. */
    data object SourceDisabled : StartResult

    /** Android refused to start the foreground service. */
    data class Failed(
        val cause: Throwable
    ) : StartResult
}
