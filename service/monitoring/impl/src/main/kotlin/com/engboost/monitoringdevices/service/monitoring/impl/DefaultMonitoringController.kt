package com.engboost.monitoringdevices.service.monitoring.impl

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanEvent
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanner
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import com.engboost.monitoringdevices.service.monitoring.api.MonitoringController
import com.engboost.monitoringdevices.service.monitoring.api.MonitoringState
import com.engboost.monitoringdevices.service.monitoring.api.MonitoringType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update

private const val PREFERENCES_NAME = "monitoring_service"
private const val ACTIVE_TYPES_KEY = "active_types"
private const val LOG_TAG = "MonitoringController"

internal class DefaultMonitoringController(
    context: Context,
    wifiScanner: WifiScanner,
    bluetoothScanner: BluetoothScanner
) : MonitoringController {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val wifiPermissions = wifiScanner.requiredPermissions.withNotificationPermission()
    private val bluetoothPermissions = bluetoothScanner.requiredPermissions.withNotificationPermission()
    private val mutableWifiEvents = MutableStateFlow<WifiScanEvent?>(null)
    private val mutableBluetoothEvents = MutableStateFlow<BluetoothScanEvent?>(null)
    private val mutableState = MutableStateFlow(
        MonitoringState(
            activeTypes = readActiveTypes(),
            wifiNetworkCount = 0,
            bluetoothDeviceCount = 0
        )
    )

    override val state = mutableState
    override val wifiEvents: Flow<WifiScanEvent> = mutableWifiEvents.filterNotNull()
    override val bluetoothEvents: Flow<BluetoothScanEvent> = mutableBluetoothEvents.filterNotNull()

    init {
        Log.i(LOG_TAG, "Initialized: activeTypes=${state.value.activeTypes}")
    }

    override fun startMonitoring(type: MonitoringType): Set<String> {
        Log.i(LOG_TAG, "Start requested: type=$type, activeTypes=${state.value.activeTypes}")
        val missingPermissions = permissionsFor(type).filterTo(linkedSetOf()) { permission ->
            appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            Log.w(LOG_TAG, "Start blocked: type=$type, missingPermissions=$missingPermissions")
            return missingPermissions
        }

        val wasActive = type in state.value.activeTypes
        if (!wasActive) clearEvent(type)
        updateActiveTypes(state.value.activeTypes + type)
        try {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, DeviceMonitoringService::class.java)
            )
        } catch (error: Throwable) {
            Log.e(LOG_TAG, "Foreground service start failed: type=$type", error)
            if (!wasActive) {
                updateActiveTypes(
                    activeTypes = state.value.activeTypes - type,
                    clearRemovedEvents = false
                )
            }
            throw error
        }
        return emptySet()
    }

    override fun stopMonitoring(type: MonitoringType) {
        Log.i(LOG_TAG, "Stop requested: type=$type")
        updateActiveTypes(state.value.activeTypes - type)
    }

    override fun stopAllMonitoring() {
        Log.i(LOG_TAG, "Stop all requested")
        updateActiveTypes(emptySet())
    }

    fun onServiceStartFailed(error: Throwable) {
        Log.e(LOG_TAG, "Foreground promotion failed", error)
        state.value.activeTypes.forEach { type -> publishStartError(type, error) }
        updateActiveTypes(emptySet(), clearRemovedEvents = false)
    }

    fun onScanCompleted(type: MonitoringType) {
        Log.i(LOG_TAG, "Scan completed: type=$type")
        updateActiveTypes(
            activeTypes = state.value.activeTypes - type,
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

    fun publishBluetoothEvent(event: BluetoothScanEvent) {
        logBluetoothEvent(event)
        mutableBluetoothEvents.value = event
        if (event is BluetoothScanEvent.Devices) {
            mutableState.update { currentState ->
                currentState.copy(bluetoothDeviceCount = event.devices.size)
            }
        }
    }

    private fun updateActiveTypes(
        activeTypes: Set<MonitoringType>,
        clearRemovedEvents: Boolean = true
    ) {
        val previousTypes = state.value.activeTypes
        if (previousTypes == activeTypes) return

        preferences.edit()
            .putStringSet(ACTIVE_TYPES_KEY, activeTypes.mapTo(mutableSetOf()) { type -> type.name })
            .apply()

        if (clearRemovedEvents) {
            (previousTypes - activeTypes).forEach(::clearEvent)
        }

        mutableState.update { currentState ->
            currentState.copy(
                activeTypes = activeTypes,
                wifiNetworkCount = if (MonitoringType.WIFI in activeTypes) {
                    currentState.wifiNetworkCount
                } else {
                    0
                },
                bluetoothDeviceCount = if (MonitoringType.BLUETOOTH in activeTypes) {
                    currentState.bluetoothDeviceCount
                } else {
                    0
                }
            )
        }
        Log.i(LOG_TAG, "Active types changed: $previousTypes -> $activeTypes")
    }

    private fun readActiveTypes(): Set<MonitoringType> {
        return preferences.getStringSet(ACTIVE_TYPES_KEY, emptySet())
            .orEmpty()
            .mapNotNullTo(linkedSetOf()) { name ->
                MonitoringType.entries.find { type -> type.name == name }
            }
    }

    private fun permissionsFor(type: MonitoringType): Set<String> {
        return when (type) {
            MonitoringType.WIFI -> wifiPermissions
            MonitoringType.BLUETOOTH -> bluetoothPermissions
        }
    }

    private fun Set<String>.withNotificationPermission(): Set<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this + Manifest.permission.POST_NOTIFICATIONS
        } else {
            this
        }
    }

    private fun publishStartError(type: MonitoringType, error: Throwable) {
        when (type) {
            MonitoringType.WIFI -> publishWifiEvent(
                WifiScanEvent.Error("Failed to start Wi-Fi monitoring", error)
            )

            MonitoringType.BLUETOOTH -> publishBluetoothEvent(
                BluetoothScanEvent.Error("Failed to start Bluetooth monitoring", error)
            )
        }
    }

    private fun clearEvent(type: MonitoringType) {
        when (type) {
            MonitoringType.WIFI -> mutableWifiEvents.value = null
            MonitoringType.BLUETOOTH -> mutableBluetoothEvents.value = null
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

    private fun logBluetoothEvent(event: BluetoothScanEvent) {
        when (event) {
            BluetoothScanEvent.Scanning -> Log.d(LOG_TAG, "Bluetooth scanner started")
            is BluetoothScanEvent.Devices -> {
                if (event.devices.size != state.value.bluetoothDeviceCount) {
                    Log.d(LOG_TAG, "Bluetooth result: devices=${event.devices.size}")
                }
            }
            is BluetoothScanEvent.PermissionRequired -> {
                Log.w(LOG_TAG, "Bluetooth scanner requires permissions=${event.permissions}")
            }
            is BluetoothScanEvent.Unavailable -> {
                Log.w(LOG_TAG, "Bluetooth unavailable: ${event.reason}")
            }
            is BluetoothScanEvent.Error -> {
                logError("Bluetooth error: ${event.message}", event.cause)
            }
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
