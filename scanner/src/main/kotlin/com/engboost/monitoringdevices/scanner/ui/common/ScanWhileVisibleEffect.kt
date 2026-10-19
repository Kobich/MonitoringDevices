package com.engboost.monitoringdevices.scanner.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
internal fun ScanWhileVisibleEffect(scan: suspend () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentScan by rememberUpdatedState(scan)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            currentScan()
        }
    }
}
