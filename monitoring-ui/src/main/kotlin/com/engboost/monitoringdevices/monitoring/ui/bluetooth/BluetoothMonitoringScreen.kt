package com.engboost.monitoringdevices.monitoring.ui.bluetooth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.ui.common.PermissionRequestEffect
import com.engboost.monitoringdevices.monitoring.ui.common.ScanWhileVisibleEffect

/**
 * Test screen of Bluetooth monitoring.
 *
 * Scans through [monitoring] after Start while the screen is visible, requests missing permissions
 * and shows found devices with sorting and a details sheet.
 */
@Composable
fun BluetoothMonitoringScreen(
    monitoring: DeviceMonitoring,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel { BluetoothViewModel(monitoring) }
    val state = viewModel.uiState

    ScanWhileVisibleEffect(scan = viewModel::scanWhileRequested)
    PermissionRequestEffect(
        request = state.permissionRequest,
        onResult = viewModel::onPermissionResult
    )

    BluetoothScreen(
        state = state,
        onStartClick = viewModel::onStartClick,
        onStopClick = viewModel::onStopClick,
        onSortModeChange = viewModel::onSortModeChange,
        onDeviceClick = viewModel::onDeviceClick,
        onDeviceDetailsDismiss = viewModel::onDeviceDetailsDismiss,
        modifier = modifier
    )
}
