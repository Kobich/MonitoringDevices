package com.engboost.monitoringdevices.monitoring.internal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.DeviceMonitoringConfig
import com.engboost.monitoringdevices.monitoring.MonitoringSource
import com.engboost.monitoringdevices.monitoring.MonitoringState
import com.engboost.monitoringdevices.monitoring.StartResult
import com.engboost.monitoringdevices.monitoring.ble.BleScanEvent
import com.engboost.monitoringdevices.monitoring.internal.ble.BleScanner
import com.engboost.monitoringdevices.monitoring.internal.wifi.WifiScanner
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update

private const val PREFERENCES_NAME = "device_monitoring"
private const val ACTIVE_SOURCES_KEY = "active_sources"
private const val LOG_TAG = "DeviceMonitoring"

internal class DefaultDeviceMonitoring(
    context: Context,
    override val config: DeviceMonitoringConfig,
    val wifiScanner: WifiScanner,
    val bleScanner: BleScanner
) : DeviceMonitoring {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val wifiPermissions = wifiScanner.requiredPermissions.withNotificationPermission()
    private val blePermissions = bleScanner.requiredPermissions.withNotificationPermission()
    private val mutableWifiEvents = MutableStateFlow<WifiScanEvent?>(null)
    private val mutableBleEvents = MutableStateFlow<BleScanEvent?>(null)
    private val mutableState = MutableStateFlow(
        MonitoringState(
            activeSources = readActiveSources(),
            wifiNetworkCount = 0,
            bleDeviceCount = 0
        )
    )

    override val state: StateFlow<MonitoringState> = mutableState.asStateFlow()
    override val isWifiScanThrottlingEnabled: Boolean
        get() = wifiScanner.isScanThrottlingEnabled

    init {
        Log.i(LOG_TAG, "Initialized: activeSources=${state.value.activeSources}, config=$config")
    }

    override fun requiredPermissions(source: MonitoringSource): Set<String> {
        return when (source) {
            MonitoringSource.WIFI -> wifiPermissions
            MonitoringSource.BLE -> blePermissions
        }
    }

    override fun missingPermissions(source: MonitoringSource): Set<String> {
        return requiredPermissions(source).filterTo(linkedSetOf()) { permission ->
            appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    override fun start(source: MonitoringSource): StartResult {
        Log.i(LOG_TAG, "Start requested: source=$source, activeSources=${state.value.activeSources}")
        if (source !in config.enabledSources) {
            Log.w(LOG_TAG, "Start ignored: source=$source is disabled by config")
            return StartResult.SourceDisabled
        }

        val missingPermissions = missingPermissions(source)
        if (missingPermissions.isNotEmpty()) {
            Log.w(LOG_TAG, "Start blocked: source=$source, missingPermissions=$missingPermissions")
            return StartResult.PermissionsRequired(missingPermissions)
        }

        val wasActive = source in state.value.activeSources
        if (!wasActive) clearEvent(source)
        updateActiveSources(state.value.activeSources + source)
        return try {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, DeviceMonitoringService::class.java)
            )
            StartResult.Started
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Foreground service start failed: source=$source", error)
            if (!wasActive) {
                updateActiveSources(
                    activeSources = state.value.activeSources - source,
                    clearRemovedEvents = false
                )
            }
            StartResult.Failed(error)
        }
    }

    override fun stop(source: MonitoringSource) {
        Log.i(LOG_TAG, "Stop requested: source=$source")
        updateActiveSources(state.value.activeSources - source)
    }

    override fun stopAll() {
        Log.i(LOG_TAG, "Stop all requested")
        updateActiveSources(emptySet())
    }

    override fun wifiEvents(): Flow<WifiScanEvent> {
        return sessionEvents(MonitoringSource.WIFI, mutableWifiEvents) { event ->
            event == WifiScanEvent.Scanning || event is WifiScanEvent.Networks
        }
    }

    override fun bleEvents(): Flow<BleScanEvent> {
        return sessionEvents(MonitoringSource.BLE, mutableBleEvents) { event ->
            event == BleScanEvent.Scanning || event is BleScanEvent.Devices
        }
    }

    fun onServiceStartFailed(error: Throwable) {
        Log.e(LOG_TAG, "Foreground promotion failed", error)
        state.value.activeSources.forEach { source -> publishStartError(source, error) }
        updateActiveSources(emptySet(), clearRemovedEvents = false)
    }

    fun onScanCompleted(source: MonitoringSource) {
        Log.i(LOG_TAG, "Scan completed: source=$source")
        updateActiveSources(
            activeSources = state.value.activeSources - source,
            clearRemovedEvents = false
        )
    }

    fun publishWifiEvent(event: WifiScanEvent) {
        logWifiEvent(event)
        mutableWifiEvents.value = event
        if (event is WifiScanEvent.Networks) {
            mutableState.update { currentState ->
                currentState.copy(wifiNetworkCount = event.networks.size)
            }
        }
    }

    fun publishBleEvent(event: BleScanEvent) {
        logBleEvent(event)
        mutableBleEvents.value = event
        if (event is BleScanEvent.Devices) {
            mutableState.update { currentState ->
                currentState.copy(bleDeviceCount = event.devices.size)
            }
        }
    }

    /**
     * Events while [source] is active: the latest event is replayed, the flow completes when the
     * source stops or after the first event for which [isOngoing] is false.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <E : Any> sessionEvents(
        source: MonitoringSource,
        events: StateFlow<E?>,
        isOngoing: (E) -> Boolean
    ): Flow<E> {
        return state
            .map { currentState -> source in currentState.activeSources }
            .distinctUntilChanged()
            .flatMapLatest { isActive ->
                if (isActive) events.filterNotNull() else flowOf(null)
            }
            .takeWhile { event -> event != null }
            .filterNotNull()
            .transformWhile { event ->
                emit(event)
                isOngoing(event)
            }
    }

    private fun updateActiveSources(
        activeSources: Set<MonitoringSource>,
        clearRemovedEvents: Boolean = true
    ) {
        val previousSources = state.value.activeSources
        if (previousSources == activeSources) return

        preferences.edit()
            .putStringSet(ACTIVE_SOURCES_KEY, activeSources.mapTo(mutableSetOf()) { source -> source.name })
            .apply()

        if (clearRemovedEvents) {
            (previousSources - activeSources).forEach(::clearEvent)
        }

        mutableState.update { currentState ->
            currentState.copy(
                activeSources = activeSources,
                wifiNetworkCount = if (MonitoringSource.WIFI in activeSources) {
                    currentState.wifiNetworkCount
                } else {
                    0
                },
                bleDeviceCount = if (MonitoringSource.BLE in activeSources) {
                    currentState.bleDeviceCount
                } else {
                    0
                }
            )
        }
        Log.i(LOG_TAG, "Active sources changed: $previousSources -> $activeSources")
    }

    private fun readActiveSources(): Set<MonitoringSource> {
        return preferences.getStringSet(ACTIVE_SOURCES_KEY, emptySet())
            .orEmpty()
            .mapNotNullTo(linkedSetOf()) { name ->
                MonitoringSource.entries.find { source -> source.name == name }
            }
            .filterTo(linkedSetOf()) { source -> source in config.enabledSources }
    }

    private fun Set<String>.withNotificationPermission(): Set<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this + Manifest.permission.POST_NOTIFICATIONS
        } else {
            this
        }
    }

    private fun publishStartError(source: MonitoringSource, error: Throwable) {
        when (source) {
            MonitoringSource.WIFI -> publishWifiEvent(
                WifiScanEvent.Error("Failed to start Wi-Fi monitoring", error)
            )

            MonitoringSource.BLE -> publishBleEvent(
                BleScanEvent.Error("Failed to start BLE monitoring", error)
            )
        }
    }

    private fun clearEvent(source: MonitoringSource) {
        when (source) {
            MonitoringSource.WIFI -> mutableWifiEvents.value = null
            MonitoringSource.BLE -> mutableBleEvents.value = null
        }
    }

    private fun logWifiEvent(event: WifiScanEvent) {
        when (event) {
            WifiScanEvent.Scanning -> Log.d(LOG_TAG, "Wi-Fi scanner started")
            is WifiScanEvent.Networks -> {
                if (event.networks.size != state.value.wifiNetworkCount) {
                    Log.d(LOG_TAG, "Wi-Fi result: networks=${event.networks.size}")
                }
            }
            is WifiScanEvent.PermissionRequired -> {
                Log.w(LOG_TAG, "Wi-Fi scanner requires permissions=${event.permissions}")
            }
            is WifiScanEvent.Unavailable -> Log.w(LOG_TAG, "Wi-Fi unavailable: ${event.reason}")
            is WifiScanEvent.Error -> logError("Wi-Fi error: ${event.message}", event.cause)
        }
    }

    private fun logBleEvent(event: BleScanEvent) {
        when (event) {
            BleScanEvent.Scanning -> Log.d(LOG_TAG, "BLE scanner started")
            is BleScanEvent.Devices -> {
                if (event.devices.size != state.value.bleDeviceCount) {
                    Log.d(LOG_TAG, "BLE result: devices=${event.devices.size}")
                }
            }
            is BleScanEvent.PermissionRequired -> {
                Log.w(LOG_TAG, "BLE scanner requires permissions=${event.permissions}")
            }
            is BleScanEvent.Unavailable -> Log.w(LOG_TAG, "BLE unavailable: ${event.reason}")
            is BleScanEvent.Error -> logError("BLE error: ${event.message}", event.cause)
        }
    }

    private fun logError(message: String, cause: Throwable?) {
        if (cause == null) {
            Log.e(LOG_TAG, message)
        } else {
            Log.e(LOG_TAG, message, cause)
        }
    }
}
