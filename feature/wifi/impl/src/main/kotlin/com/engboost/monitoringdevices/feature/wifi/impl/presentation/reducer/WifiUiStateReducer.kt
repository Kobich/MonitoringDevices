package com.engboost.monitoringdevices.feature.wifi.impl.presentation.reducer

import com.engboost.monitoringdevices.feature.wifi.impl.domain.model.WifiScanResult
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.mapper.WifiNetworkUiMapper
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.PermissionRequestUi
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiUiState

internal class WifiUiStateReducer(
    private val networkUiMapper: WifiNetworkUiMapper
) {
    fun reduce(
        state: WifiUiState,
        result: WifiScanResult
    ): WifiUiState {
        return when (result) {
            WifiScanResult.Scanning -> state.copy(
                statusText = "Wi-Fi scan running",
                isScanning = true,
                permissionRequest = null
            )

            is WifiScanResult.Networks -> state.copy(
                statusText = "Wi-Fi data received",
                networks = networkUiMapper.map(result.networks),
                isScanning = true,
                permissionRequest = null
            )

            is WifiScanResult.PermissionRequired -> state.copy(
                statusText = "Permissions required",
                isScanning = false,
                permissionRequest = result.permissions.toPermissionRequest(state.permissionRequest)
            )

            is WifiScanResult.Unavailable -> state.copy(
                statusText = "Wi-Fi unavailable",
                networks = emptyList(),
                selectedNetwork = null,
                isScanning = false,
                permissionRequest = null
            )

            is WifiScanResult.Error -> state.copy(
                statusText = "Wi-Fi scan error",
                isScanning = false,
                permissionRequest = null
            )
        }
    }

    private fun Set<String>.toPermissionRequest(currentRequest: PermissionRequestUi?): PermissionRequestUi {
        return PermissionRequestUi(
            id = currentRequest.nextId(),
            permissions = toList()
        )
    }

    private fun PermissionRequestUi?.nextId(): Int = (this?.id ?: 0) + 1
}
