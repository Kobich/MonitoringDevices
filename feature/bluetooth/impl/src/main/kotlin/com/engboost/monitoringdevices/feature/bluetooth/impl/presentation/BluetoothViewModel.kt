package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceInfo
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanEvent
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class BluetoothViewModel(
    private val scanner: BluetoothScanner
) : ViewModel() {
    private var scanJob: Job? = null

    var uiState by mutableStateOf(
        BluetoothUiState(
            statusText = "Stopped",
            permissionText = scanner.requiredPermissions.joinToString(),
            signalText = "No data",
            detectedCount = 0,
            isScanning = false,
            permissionRequest = null
        )
    )
        private set

    fun onStartClick() {
        if (scanJob?.isActive == true) return

        uiState = uiState.copy(
            statusText = "Starting Bluetooth scan",
            signalText = "Waiting for devices",
            isScanning = true,
            permissionRequest = null
        )

        scanJob = viewModelScope.launch {
            scanner.scan()
                .catch { error ->
                    uiState = uiState.copy(
                        statusText = "Bluetooth scan error",
                        signalText = error.toDisplayMessage(),
                        isScanning = false,
                        permissionRequest = null
                    )
                }
                .onCompletion { scanJob = null }
                .collect { event -> handleScanEvent(event) }
        }
    }

    fun onStopClick() {
        scanJob?.cancel()
        scanJob = null
        uiState = uiState.copy(
            statusText = "Stopped",
            signalText = "No data",
            isScanning = false,
            permissionRequest = null
        )
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        val deniedPermissions = result
            .filterValues { isGranted -> !isGranted }
            .keys

        if (result.isNotEmpty() && deniedPermissions.isEmpty()) {
            uiState = uiState.copy(permissionRequest = null)
            onStartClick()
        } else {
            uiState = uiState.copy(
                statusText = "Permissions denied",
                signalText = deniedPermissions
                    .takeIf { permissions -> permissions.isNotEmpty() }
                    ?.joinToString()
                    ?: "Permission request cancelled",
                isScanning = false,
                permissionRequest = null
            )
        }
    }

    private fun handleScanEvent(event: BluetoothScanEvent) {
        when (event) {
            BluetoothScanEvent.Scanning -> uiState = uiState.copy(
                statusText = "Bluetooth scan running",
                isScanning = true,
                permissionRequest = null
            )

            is BluetoothScanEvent.Devices -> uiState = uiState.copy(
                statusText = "Bluetooth data received",
                signalText = event.devices.bestSignalText(),
                detectedCount = event.devices.size,
                isScanning = true,
                permissionRequest = null
            )

            is BluetoothScanEvent.PermissionRequired -> uiState = uiState.copy(
                statusText = "Permissions required",
                signalText = event.permissions.joinToString(),
                isScanning = false,
                permissionRequest = event.permissions.toPermissionRequest()
            )

            is BluetoothScanEvent.Unavailable -> uiState = uiState.copy(
                statusText = "Bluetooth unavailable",
                signalText = event.reason,
                detectedCount = 0,
                isScanning = false,
                permissionRequest = null
            )

            is BluetoothScanEvent.Error -> uiState = uiState.copy(
                statusText = "Bluetooth scan error",
                signalText = event.cause
                    ?.let { cause -> "${event.message}: ${cause.toDisplayMessage()}" }
                    ?: event.message,
                isScanning = false,
                permissionRequest = null
            )
        }
    }

    private fun List<BluetoothDeviceInfo>.bestSignalText(): String {
        val device = maxByOrNull { it.rssiDbm ?: Int.MIN_VALUE } ?: return "No devices found"
        val name = device.name ?: device.address
        val rssi = device.rssiDbm?.let { "$it dBm" } ?: "RSSI unavailable"
        return "$name / $rssi / ${device.type}"
    }

    private fun Set<String>.toPermissionRequest(): PermissionRequestUi {
        return PermissionRequestUi(
            id = uiState.permissionRequest.nextId(),
            permissions = toList()
        )
    }

    private fun PermissionRequestUi?.nextId(): Int = (this?.id ?: 0) + 1

    private fun Throwable.toDisplayMessage(): String {
        return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
    }
}

@Immutable
data class BluetoothUiState(
    val statusText: String,
    val permissionText: String,
    val signalText: String,
    val detectedCount: Int,
    val isScanning: Boolean,
    val permissionRequest: PermissionRequestUi?
)

@Immutable
data class PermissionRequestUi(
    val id: Int,
    val permissions: List<String>
) {
    fun asArray(): Array<String> {
        return permissions.toTypedArray()
    }
}
