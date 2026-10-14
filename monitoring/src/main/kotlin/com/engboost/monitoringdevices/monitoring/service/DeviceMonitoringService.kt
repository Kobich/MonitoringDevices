package com.engboost.monitoringdevices.monitoring.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.engboost.monitoringdevices.monitoring.DefaultDeviceMonitoring
import com.engboost.monitoringdevices.monitoring.MonitoringSource
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothScanEvent
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private const val NOTIFICATION_ID = 1001
private const val LOG_TAG = "DeviceMonitoringService"

/**
 * Foreground service that runs one scan job per active source.
 *
 * The service follows [DefaultDeviceMonitoring.state]: it starts and cancels scan jobs, updates
 * the foreground service type and the notification, and stops itself with the last source.
 */
internal class DeviceMonitoringService : Service() {
    private val monitoring by lazy { DefaultDeviceMonitoring.get(applicationContext) }
    private val notification by lazy { MonitoringNotification(this, monitoring.config.notification) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scanJobs = mutableMapOf<MonitoringSource, Job>()
    private var stateJob: Job? = null
    private var foregroundSources: Set<MonitoringSource>? = null

    override fun onCreate() {
        super.onCreate()
        notification.createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val activeSources = monitoring.state.value.activeSources
        Log.d(LOG_TAG, "onStartCommand: action=${intent?.action}, activeSources=$activeSources")

        if (intent?.action == ACTION_STOP_ALL) {
            monitoring.stopAll()
            stopMonitoringService()
            return START_NOT_STICKY
        }

        if (activeSources.isEmpty() || !updateForeground(activeSources)) {
            stopSelf()
            return START_NOT_STICKY
        }

        observeActiveSources()
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeActiveSources() {
        if (stateJob?.isActive == true) return

        stateJob = serviceScope.launch {
            monitoring.state.collect { state ->
                syncScanJobs(state.activeSources)
                if (state.activeSources.isEmpty()) {
                    stopMonitoringService()
                } else if (updateForeground(state.activeSources)) {
                    notification.update(NOTIFICATION_ID, state)
                }
            }
        }
    }

    private fun syncScanJobs(activeSources: Set<MonitoringSource>) {
        (scanJobs.keys - activeSources).forEach { source ->
            Log.i(LOG_TAG, "Stopping scan job: source=$source")
            scanJobs.remove(source)?.cancel()
        }

        for (source in activeSources) {
            if (scanJobs[source]?.isActive == true) continue
            Log.i(LOG_TAG, "Starting scan job: source=$source")
            scanJobs[source] = serviceScope.launch {
                when (source) {
                    MonitoringSource.WIFI -> collectWifiEvents()
                    MonitoringSource.BLUETOOTH -> collectBluetoothEvents()
                }
            }
        }
    }

    private suspend fun collectWifiEvents() {
        val scanner = monitoring.wifiScanner
        val config = monitoring.config.wifi
        scanner.scan { config.refreshIntervalMillis(scanner.isScanThrottlingEnabled) }
            .catch { error -> emit(WifiScanEvent.Error("Wi-Fi background scan failed", error)) }
            .completeSourceOnFinish(MonitoringSource.WIFI)
            .collect(monitoring::publishWifiEvent)
    }

    private suspend fun collectBluetoothEvents() {
        monitoring.bluetoothScanner.scan()
            .catch { error -> emit(BluetoothScanEvent.Error("Bluetooth background scan failed", error)) }
            .completeSourceOnFinish(MonitoringSource.BLUETOOTH)
            .collect(monitoring::publishBluetoothEvent)
    }

    /** Removes [source] from active sources when the scanner finishes by itself, not by cancellation. */
    private fun <E> Flow<E>.completeSourceOnFinish(source: MonitoringSource): Flow<E> {
        return onCompletion { cause ->
            if (cause == null) {
                // Let collectors receive the terminal event before the source becomes inactive.
                yield()
                monitoring.onScanCompleted(source)
            }
        }
    }

    private fun updateForeground(activeSources: Set<MonitoringSource>): Boolean {
        if (foregroundSources == activeSources) return true

        return runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification.build(monitoring.state.value),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) activeSources.toForegroundServiceType() else 0
            )
            foregroundSources = activeSources
        }.onFailure(monitoring::onServiceStartFailed).isSuccess
    }

    private fun stopMonitoringService() {
        if (foregroundSources != null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundSources = null
        }
        stopSelf()
    }

    private fun Set<MonitoringSource>.toForegroundServiceType(): Int {
        return fold(0) { serviceType, source ->
            serviceType or when (source) {
                MonitoringSource.WIFI -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                MonitoringSource.BLUETOOTH -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            }
        }
    }

    companion object {
        const val ACTION_STOP_ALL = "com.engboost.monitoringdevices.monitoring.action.STOP_ALL"
    }
}
