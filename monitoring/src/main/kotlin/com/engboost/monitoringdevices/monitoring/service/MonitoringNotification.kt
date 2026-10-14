package com.engboost.monitoringdevices.monitoring.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.engboost.monitoringdevices.monitoring.MonitoringNotificationConfig
import com.engboost.monitoringdevices.monitoring.MonitoringSource
import com.engboost.monitoringdevices.monitoring.MonitoringState
import com.engboost.monitoringdevices.monitoring.R

private const val CHANNEL_ID = "device_monitoring"
private const val STOP_REQUEST_CODE = 1002
private const val OPEN_APP_REQUEST_CODE = 1003

/** Builds the foreground notification of [DeviceMonitoringService]. */
internal class MonitoringNotification(
    private val service: Service,
    private val config: MonitoringNotificationConfig
) {
    private val notificationManager = service.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                service.getString(R.string.device_monitoring_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    fun build(state: MonitoringState): Notification {
        val contentIntent = service.packageManager.getLaunchIntentForPackage(service.packageName)?.let { intent ->
            PendingIntent.getActivity(
                service,
                OPEN_APP_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val stopIntent = PendingIntent.getService(
            service,
            STOP_REQUEST_CODE,
            Intent(service, DeviceMonitoringService::class.java).setAction(DeviceMonitoringService.ACTION_STOP_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(service, CHANNEL_ID)
            .setSmallIcon(config.smallIconRes ?: R.drawable.device_monitoring_ic_notification)
            .setContentTitle(title(state.activeSources))
            .setContentText(text(state))
            .setContentIntent(contentIntent)
            .addAction(0, service.getString(R.string.device_monitoring_notification_action_stop_all), stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    fun update(notificationId: Int, state: MonitoringState) {
        notificationManager.notify(notificationId, build(state))
    }

    private fun title(activeSources: Set<MonitoringSource>): String {
        val titleRes = when (activeSources) {
            setOf(MonitoringSource.WIFI) -> R.string.device_monitoring_notification_title_wifi
            setOf(MonitoringSource.BLUETOOTH) -> R.string.device_monitoring_notification_title_bluetooth
            else -> R.string.device_monitoring_notification_title_all
        }
        return service.getString(titleRes)
    }

    private fun text(state: MonitoringState): String {
        return buildList {
            if (MonitoringSource.WIFI in state.activeSources) {
                add(service.getString(R.string.device_monitoring_notification_text_wifi, state.wifiNetworkCount))
            }
            if (MonitoringSource.BLUETOOTH in state.activeSources) {
                add(service.getString(R.string.device_monitoring_notification_text_bluetooth, state.bluetoothDeviceCount))
            }
        }.joinToString(" | ")
    }
}
