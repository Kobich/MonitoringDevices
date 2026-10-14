package com.engboost.monitoringdevices.monitoring.ui.wifi

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.ui.common.PermissionRequestEffect

/**
 * Test screen of Wi-Fi monitoring.
 *
 * Starts and stops monitoring through [monitoring], requests missing permissions, warns about
 * scan throttling and shows found networks with a details sheet.
 */
@Composable
fun WifiMonitoringScreen(
    monitoring: DeviceMonitoring,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel { WifiViewModel(monitoring) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = viewModel.uiState

    // Throttling can be toggled in Developer options while the app is in the background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshThrottlingStatus()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PermissionRequestEffect(
        request = state.permissionRequest,
        onResult = viewModel::onPermissionResult
    )

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
