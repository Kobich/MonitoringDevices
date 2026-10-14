package com.engboost.monitoringdevices.monitoring.ui.wifi

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring

/** Test screen for Wi-Fi monitoring: start/stop, permission requests and the network list. */
@Composable
fun WifiMonitoringScreen(
    monitoring: DeviceMonitoring,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel { WifiViewModel(monitoring) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = viewModel.uiState
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = viewModel::onPermissionResult
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshThrottlingStatus()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
        onOpenDeveloperOptionsClick = {
            viewModel.onScanThrottlingDialogDismiss()
            context.openDeveloperOptions()
        },
        onScanThrottlingDialogDismiss = viewModel::onScanThrottlingDialogDismiss,
        modifier = modifier
    )
}

private fun android.content.Context.openDeveloperOptions() {
    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
    val fallbackIntent = Intent(Settings.ACTION_SETTINGS)

    runCatching { startActivity(intent) }
        .onFailure { runCatching { startActivity(fallbackIntent) } }
}
