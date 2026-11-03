package com.engboost.monitoringdevices.core.commonui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * `true` only after [visible] stayed `true` for [delayMillis]. Use for progress indicators
 * of operations that are usually instant, so they don't flash for a frame.
 */
@Composable
fun rememberDelayedVisibility(visible: Boolean, delayMillis: Long = 300): Boolean {
    var isDelayPassed by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMillis)
            isDelayPassed = true
        } else {
            isDelayPassed = false
        }
    }
    return visible && isDelayPassed
}
