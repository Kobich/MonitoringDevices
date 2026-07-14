package com.engboost.monitoringdevices.feature.wifi.impl.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.screen.WifiScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WifiFeatureScreen(
    modifier: Modifier = Modifier,
    viewModel: WifiViewModel = koinViewModel()
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

    WifiScreen(
        state = state,
        onStartClick = viewModel::onStartClick,
        onStopClick = viewModel::onStopClick,
        onNetworkClick = viewModel::onNetworkClick,
        onNetworkDetailsDismiss = viewModel::onNetworkDetailsDismiss,
        modifier = modifier
    )
}
