package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.screen.BluetoothScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun BluetoothFeatureScreen(
    modifier: Modifier = Modifier,
    viewModel: BluetoothViewModel = koinViewModel()
) {
    val state = viewModel.uiState
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = viewModel::onPermissionResult
    )

    LaunchedEffect(state.permissionRequest?.id) {
        val request = state.permissionRequest ?: return@LaunchedEffect
        val permissions = request.asArray()
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions)
        }
    }

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
