package com.engboost.monitoringdevices.service.monitoring.impl

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
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanEvent
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanner
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanConfig
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import com.engboost.monitoringdevices.service.monitoring.api.MonitoringState
import com.engboost.monitoringdevices.service.monitoring.api.MonitoringType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val ACTION_STOP_ALL = "com.engboost.monitoringdevices.monitoring.STOP_ALL"
private const val NOTIFICATION_CHANNEL_ID = "device_monitoring"
private const val NOTIFICATION_ID = 1001
private const val STOP_REQUEST_CODE = 1002
private const val OPEN_APP_REQUEST_CODE = 1003
private const val THROTTLED_WIFI_REFRESH_INTERVAL_MILLIS = 30_000L
private const val UNTHROTTLED_WIFI_REFRESH_INTERVAL_MILLIS = 5_000L
private const val LOG_TAG = "MonitoringService"

class DeviceMonitoringService : Service(), KoinComponent {
    private val controller: DefaultMonitoringController by inject()
    private val wifiScanner: WifiScanner by inject()
    private val bluetoothScanner: BluetoothScanner by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scanJobs = mutableMapOf<MonitoringType, Job>()
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private var stateJob: Job? = null
    private var isForeground = false
    private var foregroundTypes = emptySet<MonitoringType>()

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            LOG_TAG,
            "onStartCommand: action=${intent?.action}, startId=$startId, " +
                "activeTypes=${controller.state.value.activeTypes}"
        )
        if (intent?.action == ACTION_STOP_ALL) {
            Log.i(LOG_TAG, "Notification action: stop all")
            controller.stopAllMonitoring()
            stopMonitoringService()
            return START_NOT_STICKY
        }

        if (controller.state.value.activeTypes.isEmpty()) {
            Log.w(LOG_TAG, "Start ignored: no active scan types")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!updateForeground(controller.state.value.activeTypes)) {
            stopSelf()
            return START_NOT_STICKY
        }

        observeActiveTypes()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "onDestroy: scanJobs=${scanJobs.keys}")
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeActiveTypes() {
        if (stateJob?.isActive == true) return

        stateJob = serviceScope.launch {
            controller.state.collect { state ->
                syncScanJobs(state.activeTypes)
                if (state.activeTypes.isEmpty()) {
                    stopMonitoringService()
                } else if (updateForeground(state.activeTypes)) {
                    notificationManager.notify(NOTIFICATION_ID, buildNotification())
                }
            }
        }
    }

    private fun syncScanJobs(activeTypes: Set<MonitoringType>) {
        (scanJobs.keys - activeTypes).forEach { type ->
            Log.i(LOG_TAG, "Stopping scan job: type=$type")
            scanJobs.remove(type)?.cancel()
        }

        if (MonitoringType.WIFI in activeTypes && scanJobs[MonitoringType.WIFI]?.isActive != true) {
            Log.i(LOG_TAG, "Starting scan job: type=${MonitoringType.WIFI}")
            scanJobs[MonitoringType.WIFI] = serviceScope.launch { collectWifiResults() }
        }
        if (
            MonitoringType.BLUETOOTH in activeTypes &&
            scanJobs[MonitoringType.BLUETOOTH]?.isActive != true
        ) {
            Log.i(LOG_TAG, "Starting scan job: type=${MonitoringType.BLUETOOTH}")
            scanJobs[MonitoringType.BLUETOOTH] = serviceScope.launch { collectBluetoothResults() }
        }
    }

    private suspend fun collectWifiResults() {
        val isScanThrottlingEnabled = wifiScanner.isScanThrottlingEnabled
        val refreshIntervalMillis = if (isScanThrottlingEnabled) {
            THROTTLED_WIFI_REFRESH_INTERVAL_MILLIS
        } else {
            UNTHROTTLED_WIFI_REFRESH_INTERVAL_MILLIS
        }
        Log.d(
            LOG_TAG,
            "Wi-Fi collection started: throttling=$isScanThrottlingEnabled, " +
                "intervalMs=$refreshIntervalMillis"
        )
        wifiScanner.scan(WifiScanConfig(refreshIntervalMillis = refreshIntervalMillis))
            .catch { error ->
                Log.e(LOG_TAG, "Wi-Fi collection failed", error)
                emit(WifiScanEvent.Error("Wi-Fi background scan failed", error))
            }
            .onCompletion { cause ->
                if (cause == null) {
                    Log.i(LOG_TAG, "Wi-Fi collection completed")
                    yield()
                    controller.onScanCompleted(MonitoringType.WIFI)
                } else {
                    Log.d(LOG_TAG, "Wi-Fi collection cancelled")
                }
            }
            .collect(controller::publishWifiEvent)
    }

    private suspend fun collectBluetoothResults() {
        Log.d(LOG_TAG, "Bluetooth collection started")
        bluetoothScanner.scan()
            .catch { error ->
                Log.e(LOG_TAG, "Bluetooth collection failed", error)
                emit(BluetoothScanEvent.Error("Bluetooth background scan failed", error))
            }
            .onCompletion { cause ->
                if (cause == null) {
                    Log.i(LOG_TAG, "Bluetooth collection completed")
                    yield()
                    controller.onScanCompleted(MonitoringType.BLUETOOTH)
                } else {
                    Log.d(LOG_TAG, "Bluetooth collection cancelled")
                }
            }
            .collect(controller::publishBluetoothEvent)
    }

    private fun updateForeground(activeTypes: Set<MonitoringType>): Boolean {
        if (isForeground && foregroundTypes == activeTypes) return true

        Log.i(LOG_TAG, "Updating foreground: activeTypes=$activeTypes")
        return runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activeTypes.toForegroundServiceType()
                } else {
                    0
                }
            )
            isForeground = true
            foregroundTypes = activeTypes
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
            .setSmallIcon(R.drawable.ic_monitoring)
            .setContentTitle(state.activeTypes.toNotificationTitle())
            .setContentText(state.toNotificationText())
            .setContentIntent(contentIntent)
            .addAction(0, "Stop all", stopIntent)
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
                "Device monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        Log.d(LOG_TAG, "Notification channel ready: id=$NOTIFICATION_CHANNEL_ID")
    }

    @Suppress("DEPRECATION")
    private fun stopMonitoringService() {
        Log.i(LOG_TAG, "Stopping service: activeTypes=${controller.state.value.activeTypes}")
        if (isForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
            isForeground = false
            foregroundTypes = emptySet()
        }
        stopSelf()
    }

    private fun Set<MonitoringType>.toNotificationTitle(): String {
        return when (this) {
            setOf(MonitoringType.WIFI) -> "Wi-Fi monitoring active"
            setOf(MonitoringType.BLUETOOTH) -> "Bluetooth monitoring active"
            else -> "Device monitoring active"
        }
    }

    private fun Set<MonitoringType>.toForegroundServiceType(): Int {
        var serviceType = 0
        if (MonitoringType.WIFI in this) {
            serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        if (MonitoringType.BLUETOOTH in this) {
            serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        }
        return serviceType
    }

    private fun MonitoringState.toNotificationText(): String {
        return buildList {
            if (MonitoringType.WIFI in activeTypes) {
                add("Wi-Fi: $wifiNetworkCount networks")
            }
            if (MonitoringType.BLUETOOTH in activeTypes) {
                add("Bluetooth: $bluetoothDeviceCount devices")
            }
        }.joinToString(" | ")
    }
}
