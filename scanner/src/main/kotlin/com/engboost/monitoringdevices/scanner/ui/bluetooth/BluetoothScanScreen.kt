package com.engboost.monitoringdevices.scanner.ui.bluetooth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.engboost.monitoringdevices.scanner.bluetooth.BluetoothScannerApi
import com.engboost.monitoringdevices.scanner.ui.common.PermissionRequestEffect
import com.engboost.monitoringdevices.scanner.ui.common.ScanWhileVisibleEffect

/** Scans only after Start and only while the screen is visible. */
@Composable
fun BluetoothScanScreen(
    scanner: BluetoothScannerApi,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel { BluetoothViewModel(scanner) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScanWhileVisibleEffect(scan = viewModel::scanWhileVisible)
    PermissionRequestEffect(
        request = state.permissionRequest,
        onResult = viewModel::onPermissionResult
    )

    BluetoothScanContent(
        state = state,
        onStartClick = viewModel::onStartClick,
        onStopClick = viewModel::onStopClick,
        onSortModeChange = viewModel::onSortModeChange,
        onDeviceClick = viewModel::onDeviceClick,
        onDeviceDetailsDismiss = viewModel::onDeviceDetailsDismiss,
        modifier = modifier
    )
}
