package com.engboost.monitoringdevices.scanner.ui.wifi

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.engboost.monitoringdevices.scanner.ui.common.PermissionRequestEffect
import com.engboost.monitoringdevices.scanner.ui.common.ScanWhileVisibleEffect
import com.engboost.monitoringdevices.scanner.wifi.WifiScannerApi

/**
 * Test screen of the Wi-Fi scanner.
 *
 * Scans with [scanner] after Start while the screen is visible, requests missing permissions,
 * warns about scan throttling and shows found networks with a details sheet.
 */
@Composable
fun WifiScanScreen(
    scanner: WifiScannerApi,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel { WifiViewModel(scanner) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Throttling can be toggled in Developer options while the app is in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshThrottlingStatus()
        onPauseOrDispose { }
    }
    ScanWhileVisibleEffect(scan = viewModel::scanWhileVisible)
    PermissionRequestEffect(
        request = state.permissionRequest,
        onResult = viewModel::onPermissionResult
    )

    WifiScanContent(
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

private fun Context.openDeveloperOptions() {
    runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
        .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
}
