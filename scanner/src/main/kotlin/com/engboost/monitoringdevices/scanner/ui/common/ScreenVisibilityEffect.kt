package com.engboost.monitoringdevices.scanner.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation

/** Reports true when the screen becomes visible and false when it is hidden or leaves composition. */
@Composable
internal fun ScreenVisibilityEffect(onVisibilityChange: (Boolean) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnVisibilityChange by rememberUpdatedState(onVisibilityChange)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            currentOnVisibilityChange(true)
            try {
                awaitCancellation()
            } finally {
                currentOnVisibilityChange(false)
            }
        }
    }
}
