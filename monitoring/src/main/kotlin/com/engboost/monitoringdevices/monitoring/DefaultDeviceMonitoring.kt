package com.engboost.monitoringdevices.monitoring

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothScanEvent
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothScanner
import com.engboost.monitoringdevices.monitoring.service.DeviceMonitoringService
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanEvent
import com.engboost.monitoringdevices.monitoring.wifi.WifiScanner
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

/**
 * Implementation of [DeviceMonitoring].
 *
 * Keeps the set of active sources and the latest event of each source. Scanning itself runs in
 * [DeviceMonitoringService], which observes [state] and publishes scanner events back here.
 */
internal class DefaultDeviceMonitoring(
    context: Context,
    override val config: DeviceMonitoringConfig
) : DeviceMonitoring {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val wifiScanner = WifiScanner(appContext)
    val bluetoothScanner = BluetoothScanner(appContext, config.bluetooth)

    private val wifiEvents = SourceEvents<WifiScanEvent> { event ->
        event == WifiScanEvent.Scanning || event is WifiScanEvent.Networks
    }
    private val bluetoothEvents = SourceEvents<BluetoothScanEvent> { event ->
        event == BluetoothScanEvent.Scanning || event is BluetoothScanEvent.Devices
    }
    private val mutableState = MutableStateFlow(
        MonitoringState(
            activeSources = readActiveSources(),
            wifiNetworkCount = 0,
            bluetoothDeviceCount = 0
        )
    )

    override val state: StateFlow<MonitoringState> = mutableState.asStateFlow()

    override val isWifiScanThrottlingEnabled: Boolean
        get() = wifiScanner.isScanThrottlingEnabled

    override fun requiredPermissions(source: MonitoringSource): Set<String> {
        val scannerPermissions = when (source) {
            MonitoringSource.WIFI -> wifiScanner.requiredPermissions
            MonitoringSource.BLUETOOTH -> bluetoothScanner.requiredPermissions
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            scannerPermissions + Manifest.permission.POST_NOTIFICATIONS
        } else {
            scannerPermissions
        }
    }

    override fun missingPermissions(source: MonitoringSource): Set<String> {
        return requiredPermissions(source).filterTo(linkedSetOf()) { permission ->
            appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    override fun start(source: MonitoringSource): StartResult {
        Log.i(LOG_TAG, "Start requested: source=$source, activeSources=${state.value.activeSources}")
        if (source !in config.enabledSources) return StartResult.SourceDisabled

        val missingPermissions = missingPermissions(source)
        if (missingPermissions.isNotEmpty()) return StartResult.PermissionsRequired(missingPermissions)

        val wasActive = source in state.value.activeSources
        if (!wasActive) eventsOf(source).clear()
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
                updateActiveSources(state.value.activeSources - source, clearRemovedEvents = false)
            }
            StartResult.Failed(error)
        }
    }

    override fun stop(source: MonitoringSource) {
        updateActiveSources(state.value.activeSources - source)
    }

    override fun stopAll() {
        updateActiveSources(emptySet())
    }

    override fun wifiEvents(): Flow<WifiScanEvent> = wifiEvents.session(isActive(MonitoringSource.WIFI))

    override fun bluetoothEvents(): Flow<BluetoothScanEvent> {
        return bluetoothEvents.session(isActive(MonitoringSource.BLUETOOTH))
    }

    /** Called by the service when Android refused to promote it to the foreground. */
    fun onServiceStartFailed(error: Throwable) {
        Log.e(LOG_TAG, "Foreground promotion failed", error)
        state.value.activeSources.forEach { source ->
            when (source) {
                MonitoringSource.WIFI -> publishWifiEvent(WifiScanEvent.Error("Failed to start Wi-Fi monitoring", error))
                MonitoringSource.BLUETOOTH -> publishBluetoothEvent(
                    BluetoothScanEvent.Error("Failed to start Bluetooth monitoring", error)
                )
            }
        }
        updateActiveSources(emptySet(), clearRemovedEvents = false)
    }

    /** Called by the service when the scanner of [source] finished after a terminal event. */
    fun onScanCompleted(source: MonitoringSource) {
        updateActiveSources(state.value.activeSources - source, clearRemovedEvents = false)
    }

    fun publishWifiEvent(event: WifiScanEvent) {
        when (event) {
            is WifiScanEvent.Networks -> mutableState.update { it.copy(wifiNetworkCount = event.networks.size) }
            is WifiScanEvent.Error -> Log.e(LOG_TAG, "Wi-Fi error: ${event.message}", event.cause)
            else -> Log.i(LOG_TAG, "Wi-Fi event: $event")
        }
        wifiEvents.publish(event)
    }

    fun publishBluetoothEvent(event: BluetoothScanEvent) {
        when (event) {
            is BluetoothScanEvent.Devices -> mutableState.update { it.copy(bluetoothDeviceCount = event.devices.size) }
            is BluetoothScanEvent.Error -> Log.e(LOG_TAG, "Bluetooth error: ${event.message}", event.cause)
            else -> Log.i(LOG_TAG, "Bluetooth event: $event")
        }
        bluetoothEvents.publish(event)
    }

    private fun isActive(source: MonitoringSource): Flow<Boolean> {
        return state.map { currentState -> source in currentState.activeSources }.distinctUntilChanged()
    }

    private fun eventsOf(source: MonitoringSource): SourceEvents<*> {
        return when (source) {
            MonitoringSource.WIFI -> wifiEvents
            MonitoringSource.BLUETOOTH -> bluetoothEvents
        }
    }

    /**
     * Saves [activeSources] and resets counters of stopped sources.
     *
     * Events of stopped sources are cleared unless [clearRemovedEvents] is `false`: after a terminal
     * event it has to stay available for collectors that have not received it yet.
     */
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
            (previousSources - activeSources).forEach { source -> eventsOf(source).clear() }
        }

        mutableState.update { currentState ->
            currentState.copy(
                activeSources = activeSources,
                wifiNetworkCount = currentState.wifiNetworkCount.takeIf { MonitoringSource.WIFI in activeSources } ?: 0,
                bluetoothDeviceCount = currentState.bluetoothDeviceCount
                    .takeIf { MonitoringSource.BLUETOOTH in activeSources } ?: 0
            )
        }
        Log.i(LOG_TAG, "Active sources changed: $previousSources -> $activeSources")
    }

    /** Restores sources that were active before the process was recreated. */
    private fun readActiveSources(): Set<MonitoringSource> {
        return preferences.getStringSet(ACTIVE_SOURCES_KEY, emptySet())
            .orEmpty()
            .mapNotNullTo(linkedSetOf()) { name -> MonitoringSource.entries.find { source -> source.name == name } }
            .filterTo(linkedSetOf()) { source -> source in config.enabledSources }
    }

    /**
     * Latest event of one source.
     *
     * [isOngoing] tells whether the session continues after an event; any other event is terminal.
     */
    private class SourceEvents<E : Any>(
        private val isOngoing: (E) -> Boolean
    ) {
        private val latest = MutableStateFlow<E?>(null)

        fun publish(event: E) {
            latest.value = event
        }

        fun clear() {
            latest.value = null
        }

        /**
         * Returns events while [isActive] emits `true`.
         *
         * The latest event is replayed. The flow completes when the source stops or after a terminal event.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        fun session(isActive: Flow<Boolean>): Flow<E> {
            return isActive
                .flatMapLatest { active -> if (active) latest.filterNotNull() else flowOf(null) }
                .takeWhile { event -> event != null }
                .filterNotNull()
                .transformWhile { event ->
                    emit(event)
                    isOngoing(event)
                }
        }
    }

    companion object {
        @Volatile
        private var instance: DefaultDeviceMonitoring? = null

        fun initialize(context: Context, config: DeviceMonitoringConfig): DefaultDeviceMonitoring {
            synchronized(this) {
                instance?.let { current ->
                    check(current.config == config) { "DeviceMonitoring is already initialized with a different config" }
                    return current
                }
                return DefaultDeviceMonitoring(context, config).also { instance = it }
            }
        }

        fun get(context: Context): DefaultDeviceMonitoring {
            instance?.let { return it }
            synchronized(this) {
                return instance ?: DefaultDeviceMonitoring(context, DeviceMonitoringConfig()).also { instance = it }
            }
        }
    }
}
