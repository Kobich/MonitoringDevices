package com.engboost.monitoringdevices.monitoring

import android.content.Context
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothScanEvent
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry point of the library: monitoring of nearby Wi-Fi networks and Bluetooth devices.
 *
 * Both sources run in one foreground service, but each of them is started and stopped
 * independently. Sources that are not listed in [DeviceMonitoringConfig.enabledSources]
 * cannot be started.
 *
 * Obtain the process-wide instance with [initialize] in `Application.onCreate` or with [get].
 * The interface can be bound into any DI container or replaced with a fake in tests.
 */
interface DeviceMonitoring {
    /** Configuration the instance was created with. */
    val config: DeviceMonitoringConfig

    /** Active sources and the number of found networks and devices. */
    val state: StateFlow<MonitoringState>

    /** Whether Android Wi-Fi scan throttling is enabled in Developer options. */
    val isWifiScanThrottlingEnabled: Boolean

    /**
     * Returns runtime permissions required by [source], including the notification permission
     * on Android 13 and newer.
     */
    fun requiredPermissions(source: MonitoringSource): Set<String>

    /** Returns permissions from [requiredPermissions] that are not granted yet. */
    fun missingPermissions(source: MonitoringSource): Set<String>

    /** Starts monitoring of [source] and the foreground service if it is not running yet. */
    fun start(source: MonitoringSource): StartResult

    /** Stops monitoring of [source]. The service stops together with the last active source. */
    fun stop(source: MonitoringSource)

    /** Stops monitoring of all sources and the service. */
    fun stopAll()

    /**
     * Returns events of the current Wi-Fi session.
     *
     * The last event is replayed to a new collector. The flow completes when Wi-Fi monitoring
     * stops or after a terminal event: [WifiScanEvent.PermissionRequired],
     * [WifiScanEvent.Unavailable] or [WifiScanEvent.Error].
     */
    fun wifiEvents(): Flow<WifiScanEvent>

    /**
     * Returns events of the current Bluetooth session.
     *
     * Follows the same replay and completion rules as [wifiEvents].
     */
    fun bluetoothEvents(): Flow<BluetoothScanEvent>

    companion object {
        /**
         * Creates the process-wide instance with [config] and returns it.
         *
         * Call it in `Application.onCreate`, so the service restarted by Android after process
         * death uses the same configuration. Calling it again with a different [config] throws
         * [IllegalStateException].
         */
        fun initialize(
            context: Context,
            config: DeviceMonitoringConfig = DeviceMonitoringConfig()
        ): DeviceMonitoring = DefaultDeviceMonitoring.initialize(context, config)

        /** Returns the process-wide instance, creating it with the default configuration if needed. */
        fun get(context: Context): DeviceMonitoring = DefaultDeviceMonitoring.get(context)
    }
}

/** Kind of nearby devices that can be monitored. */
enum class MonitoringSource {
    WIFI,
    BLUETOOTH
}

/**
 * Snapshot of the monitoring state.
 *
 * @property activeSources sources that are being monitored right now.
 * @property wifiNetworkCount number of networks in the last Wi-Fi result.
 * @property bluetoothDeviceCount number of devices in the last Bluetooth result.
 */
data class MonitoringState(
    val activeSources: Set<MonitoringSource>,
    val wifiNetworkCount: Int,
    val bluetoothDeviceCount: Int
)

/** Result of [DeviceMonitoring.start]. */
sealed interface StartResult {
    /** Monitoring is running; collect [DeviceMonitoring.wifiEvents] or [DeviceMonitoring.bluetoothEvents]. */
    data object Started : StartResult

    /** [permissions] must be granted before the source can be started. */
    data class PermissionsRequired(
        val permissions: Set<String>
    ) : StartResult

    /** The source is not listed in [DeviceMonitoringConfig.enabledSources]. */
    data object SourceDisabled : StartResult

    /** Android refused to start the foreground service because of [cause]. */
    data class Failed(
        val cause: Throwable
    ) : StartResult
}
