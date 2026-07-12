package com.engboost.monitoringdevices.feature.radio.impl.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.scanner.radio.api.RadioCellInfo
import com.engboost.monitoringdevices.scanner.radio.api.RadioScanEvent
import com.engboost.monitoringdevices.scanner.radio.api.RadioScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class RadioViewModel(
    private val scanner: RadioScanner
) : ViewModel() {
    private var scanJob: Job? = null

    var uiState by mutableStateOf(
        RadioUiState(
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
            statusText = "Starting radio scan",
            signalText = "Waiting for cells",
            isScanning = true,
            permissionRequest = null
        )

        scanJob = viewModelScope.launch {
            scanner.scan()
                .catch { error ->
                    uiState = uiState.copy(
                        statusText = "Radio scan error",
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

    private fun handleScanEvent(event: RadioScanEvent) {
        when (event) {
            RadioScanEvent.Scanning -> uiState = uiState.copy(
                statusText = "Radio scan running",
                isScanning = true,
                permissionRequest = null
            )

            is RadioScanEvent.Cells -> uiState = uiState.copy(
                statusText = "Radio data received",
                signalText = event.cells.bestSignalText(),
                detectedCount = event.cells.size,
                isScanning = false,
                permissionRequest = null
            )

            is RadioScanEvent.PermissionRequired -> uiState = uiState.copy(
                statusText = "Permissions required",
                signalText = event.permissions.joinToString(),
                isScanning = false,
                permissionRequest = event.permissions.toPermissionRequest()
            )

            is RadioScanEvent.Unavailable -> uiState = uiState.copy(
                statusText = "Radio unavailable",
                signalText = event.reason,
                detectedCount = 0,
                isScanning = false,
                permissionRequest = null
            )

            is RadioScanEvent.Error -> uiState = uiState.copy(
                statusText = "Radio scan error",
                signalText = event.cause
                    ?.let { cause -> "${event.message}: ${cause.toDisplayMessage()}" }
                    ?: event.message,
                isScanning = false,
                permissionRequest = null
            )
        }
    }

    private fun List<RadioCellInfo>.bestSignalText(): String {
        val cell = maxByOrNull { it.signalStrengthDbm ?: Int.MIN_VALUE } ?: return "No cells found"
        val signal = cell.signalStrengthDbm?.let { "$it dBm" } ?: "signal unavailable"
        val operator = cell.operatorName ?: "operator unavailable"
        return "${cell.networkType} / $signal / $operator"
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
data class RadioUiState(
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
