package com.engboost.monitoringdevices.monitoring

import android.content.Context
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothScanEvent
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothScanner
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothUnavailableReason
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanner
import com.engboost.monitoringdevices.monitoring.wifi.WifiUnavailableReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Implementation of [DeviceMonitoring] that delegates to the scanner of each source. */
internal class DefaultDeviceMonitoring(
    context: Context,
    override val config: DeviceMonitoringConfig
) : DeviceMonitoring {
    private val wifiScanner = WifiScanner(context)
    private val bluetoothScanner = BluetoothScanner(context, config.bluetooth)

    override val isWifiScanThrottlingEnabled: Boolean
        get() = wifiScanner.isScanThrottlingEnabled

    override fun requiredPermissions(source: MonitoringSource): Set<String> {
        return when (source) {
            MonitoringSource.WIFI -> wifiScanner.requiredPermissions
            MonitoringSource.BLUETOOTH -> bluetoothScanner.requiredPermissions
        }
    }

    override fun missingPermissions(source: MonitoringSource): Set<String> {
        return when (source) {
            MonitoringSource.WIFI -> wifiScanner.missingPermissions()
            MonitoringSource.BLUETOOTH -> bluetoothScanner.missingPermissions()
        }
    }

    override fun scanWifi(): Flow<WifiScanEvent> {
        if (MonitoringSource.WIFI !in config.enabledSources) {
            return flowOf(WifiScanEvent.Unavailable(WifiUnavailableReason.SOURCE_DISABLED))
        }
        return wifiScanner.scan {
            // Re-read on every cycle: throttling can be toggled in Developer options at any time.
            config.wifi.refreshIntervalMillis(wifiScanner.isScanThrottlingEnabled)
        }
    }

    override fun scanBluetooth(): Flow<BluetoothScanEvent> {
        if (MonitoringSource.BLUETOOTH !in config.enabledSources) {
            return flowOf(BluetoothScanEvent.Unavailable(BluetoothUnavailableReason.SOURCE_DISABLED))
        }
        return bluetoothScanner.scan()
    }
}
