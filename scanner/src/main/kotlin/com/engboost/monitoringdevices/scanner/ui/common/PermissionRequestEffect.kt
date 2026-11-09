package com.engboost.monitoringdevices.scanner.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun PermissionRequestEffect(
    request: PermissionRequestUi?,
    onResult: (Map<String, Boolean>) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = onResult
    )

    LaunchedEffect(request?.id) {
        val permissions = request?.permissions.orEmpty()
        if (permissions.isNotEmpty()) {
            launcher.launch(permissions.toTypedArray())
        }
    }
}
