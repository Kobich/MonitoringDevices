package com.engboost.monitoringdevices.monitoring.internal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.engboost.monitoringdevices.monitoring.MonitoringSource
import com.engboost.monitoringdevices.monitoring.MonitoringState
import com.engboost.monitoringdevices.monitoring.R
import com.engboost.monitoringdevices.monitoring.ble.BleScanEvent
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private const val ACTION_STOP_ALL = "com.engboost.monitoringdevices.monitoring.action.STOP_ALL"
private const val NOTIFICATION_CHANNEL_ID = "device_monitoring"
private const val NOTIFICATION_ID = 1001
private const val STOP_REQUEST_CODE = 1002
private const val OPEN_APP_REQUEST_CODE = 1003
private const val LOG_TAG = "DeviceMonitoringService"

internal class DeviceMonitoringService : Service() {
    private val controller by lazy { DeviceMonitoringProvider.get(applicationContext) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scanJobs = mutableMapOf<MonitoringSource, Job>()
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private var stateJob: Job? = null
    private var isForeground = false
    private var foregroundSources = emptySet<MonitoringSource>()

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            LOG_TAG,
            "onStartCommand: action=${intent?.action}, startId=$startId, " +
                "activeSources=${controller.state.value.activeSources}"
        )
        if (intent?.action == ACTION_STOP_ALL) {
            Log.i(LOG_TAG, "Notification action: stop all")
            controller.stopAll()
            stopMonitoringService()
            return START_NOT_STICKY
        }

        if (controller.state.value.activeSources.isEmpty()) {
            Log.w(LOG_TAG, "Start ignored: no active sources")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!updateForeground(controller.state.value.activeSources)) {
            stopSelf()
            return START_NOT_STICKY
        }

        observeActiveSources()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "onDestroy: scanJobs=${scanJobs.keys}")
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeActiveSources() {
        if (stateJob?.isActive == true) return

        stateJob = serviceScope.launch {
            controller.state.collect { state ->
                syncScanJobs(state.activeSources)
                if (state.activeSources.isEmpty()) {
                    stopMonitoringService()
                } else if (updateForeground(state.activeSources)) {
                    notificationManager.notify(NOTIFICATION_ID, buildNotification())
                }
            }
        }
    }

    private fun syncScanJobs(activeSources: Set<MonitoringSource>) {
        (scanJobs.keys - activeSources).forEach { source ->
            Log.i(LOG_TAG, "Stopping scan job: source=$source")
            scanJobs.remove(source)?.cancel()
        }

        if (MonitoringSource.WIFI in activeSources && scanJobs[MonitoringSource.WIFI]?.isActive != true) {
            Log.i(LOG_TAG, "Starting scan job: source=${MonitoringSource.WIFI}")
            scanJobs[MonitoringSource.WIFI] = serviceScope.launch { collectWifiResults() }
        }
        if (
            MonitoringSource.BLE in activeSources &&
            scanJobs[MonitoringSource.BLE]?.isActive != true
        ) {
            Log.i(LOG_TAG, "Starting scan job: source=${MonitoringSource.BLE}")
            scanJobs[MonitoringSource.BLE] = serviceScope.launch { collectBleResults() }
        }
    }

    private suspend fun collectWifiResults() {
        val wifiScanner = controller.wifiScanner
        val wifiConfig = controller.config.wifi
        Log.d(LOG_TAG, "Wi-Fi collection started: config=$wifiConfig")
        wifiScanner
            .scan {
                // Re-read on every cycle: throttling can be toggled in Developer options at any time.
                if (wifiScanner.isScanThrottlingEnabled) {
                    wifiConfig.throttledRefreshIntervalMillis
                } else {
                    wifiConfig.unthrottledRefreshIntervalMillis
                }
            }
            .catch { error ->
                Log.e(LOG_TAG, "Wi-Fi collection failed", error)
                emit(WifiScanEvent.Error("Wi-Fi background scan failed", error))
            }
            .onCompletion { cause ->
                if (cause == null) {
                    Log.i(LOG_TAG, "Wi-Fi collection completed")
                    yield()
                    controller.onScanCompleted(MonitoringSource.WIFI)
                } else {
                    Log.d(LOG_TAG, "Wi-Fi collection cancelled")
                }
            }
            .collect(controller::publishWifiEvent)
    }

    private suspend fun collectBleResults() {
        Log.d(LOG_TAG, "BLE collection started")
        controller.bleScanner.scan()
            .catch { error ->
                Log.e(LOG_TAG, "BLE collection failed", error)
                emit(BleScanEvent.Error("BLE background scan failed", error))
            }
            .onCompletion { cause ->
                if (cause == null) {
                    Log.i(LOG_TAG, "BLE collection completed")
                    yield()
                    controller.onScanCompleted(MonitoringSource.BLE)
                } else {
                    Log.d(LOG_TAG, "BLE collection cancelled")
                }
            }
            .collect(controller::publishBleEvent)
    }

    private fun updateForeground(activeSources: Set<MonitoringSource>): Boolean {
        if (isForeground && foregroundSources == activeSources) return true

        Log.i(LOG_TAG, "Updating foreground: activeSources=$activeSources")
        return runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activeSources.toForegroundServiceType()
                } else {
                    0
                }
            )
            isForeground = true
            foregroundSources = activeSources
        }.onFailure(controller::onServiceStartFailed).isSuccess
    }

    private fun buildNotification(): Notification {
        val state = controller.state.value
        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            PendingIntent.getActivity(
                this,
                OPEN_APP_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val stopIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            Intent(this, DeviceMonitoringService::class.java).setAction(ACTION_STOP_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(
                controller.config.notification.smallIconRes ?: R.drawable.device_monitoring_ic_notification
            )
            .setContentTitle(state.activeSources.toNotificationTitle())
            .setContentText(state.toNotificationText())
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.device_monitoring_notification_action_stop_all), stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.device_monitoring_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        Log.d(LOG_TAG, "Notification channel ready: id=$NOTIFICATION_CHANNEL_ID")
    }

    @Suppress("DEPRECATION")
    private fun stopMonitoringService() {
        Log.i(LOG_TAG, "Stopping service: activeSources=${controller.state.value.activeSources}")
        if (isForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
            isForeground = false
            foregroundSources = emptySet()
        }
        stopSelf()
    }

    private fun Set<MonitoringSource>.toNotificationTitle(): String {
        return when (this) {
            setOf(MonitoringSource.WIFI) -> getString(R.string.device_monitoring_notification_title_wifi)
            setOf(MonitoringSource.BLE) -> getString(R.string.device_monitoring_notification_title_ble)
            else -> getString(R.string.device_monitoring_notification_title_all)
        }
    }

    private fun Set<MonitoringSource>.toForegroundServiceType(): Int {
        var serviceType = 0
        if (MonitoringSource.WIFI in this) {
            serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        if (MonitoringSource.BLE in this) {
            serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        }
        return serviceType
    }

    private fun MonitoringState.toNotificationText(): String {
        return buildList {
            if (MonitoringSource.WIFI in activeSources) {
                add(getString(R.string.device_monitoring_notification_text_wifi, wifiNetworkCount))
            }
            if (MonitoringSource.BLE in activeSources) {
                add(getString(R.string.device_monitoring_notification_text_ble, bleDeviceCount))
            }
        }.joinToString(" | ")
    }
}
