package com.engboost.monitoringdevices.feature.wifi.impl.presentation

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
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.screen.WifiScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WifiFeatureScreen(
    modifier: Modifier = Modifier,
    viewModel: WifiViewModel = koinViewModel()
) {
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

    LaunchedEffect(state.permissionRequest) {
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
