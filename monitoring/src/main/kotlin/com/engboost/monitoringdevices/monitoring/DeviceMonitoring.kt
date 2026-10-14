package com.engboost.monitoringdevices.monitoring

import android.content.Context
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothScanEvent
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import kotlinx.coroutines.flow.Flow

/**
 * Entry point of the library: scanning of nearby Wi-Fi networks and Bluetooth devices.
 *
 * Scanning runs only while the flow returned by [scanWifi] or [scanBluetooth] is collected, so it is
 * bound to the scope of the caller, e.g. a visible screen. There is no background work.
 *
 * Create an instance with [DeviceMonitoring] and keep it in the application or a DI container.
 * The interface can be replaced with a fake in tests.
 */
interface DeviceMonitoring {
    /** Configuration the instance was created with. */
    val config: DeviceMonitoringConfig

    /** Whether Android Wi-Fi scan throttling is enabled in Developer options. */
    val isWifiScanThrottlingEnabled: Boolean

    /** Returns runtime permissions required to scan [source]. */
    fun requiredPermissions(source: MonitoringSource): Set<String>

    /** Returns permissions from [requiredPermissions] that are not granted yet. */
    fun missingPermissions(source: MonitoringSource): Set<String>

    /**
     * Returns a cold flow that scans Wi-Fi networks while it is collected.
     *
     * The flow completes after a terminal event: [WifiScanEvent.PermissionRequired],
     * [WifiScanEvent.Unavailable] or [WifiScanEvent.Error].
     */
    fun scanWifi(): Flow<WifiScanEvent>

    /**
     * Returns a cold flow that scans Bluetooth devices while it is collected.
     *
     * Follows the same completion rules as [scanWifi].
     */
    fun scanBluetooth(): Flow<BluetoothScanEvent>
}

/** Creates a [DeviceMonitoring] instance with [config]. */
fun DeviceMonitoring(
    context: Context,
    config: DeviceMonitoringConfig = DeviceMonitoringConfig()
): DeviceMonitoring = DefaultDeviceMonitoring(context, config)

/** Kind of nearby devices that can be scanned. */
enum class MonitoringSource {
    WIFI,
    BLUETOOTH
}
