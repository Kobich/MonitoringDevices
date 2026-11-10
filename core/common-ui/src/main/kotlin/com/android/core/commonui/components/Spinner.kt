package com.android.core.commonui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.core.commonui.theme.MonitoringDevicesTheme

/** One turn of the arc. Material's indeterminate indicator has no speed setting and spins too fast. */
private const val TURN_MILLIS = 1600
private const val ARC_FRACTION = 0.3f

/**
 * Indeterminate circular progress. [compact] is the small one for a place inside content
 * (a page, a picture, a button); the default size is for an empty area.
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    SpinnerArc(
        color = ProgressIndicatorDefaults.circularColor,
        strokeWidth = if (compact) 2.dp else ProgressIndicatorDefaults.CircularStrokeWidth,
        modifier = modifier.size(if (compact) 24.dp else 40.dp)
    )
}

/**
 * A fixed arc of the determinate indicator, turned at [TURN_MILLIS]. Fills the size given by [modifier].
 * Screen readers hear an indeterminate progress, not the arc's 30%.
 */
@Composable
internal fun SpinnerArc(
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    val rotation = rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = TURN_MILLIS, easing = LinearEasing)),
        label = "spinnerRotation"
    )
    Box(modifier = modifier.clearAndSetSemantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        CircularProgressIndicator(
            progress = { ARC_FRACTION },
            color = color,
            strokeWidth = strokeWidth,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
            // Read in the layer, so the turn redraws without recomposing.
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpinnerPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spinner()
            Spinner(compact = true)
        }
    }
}
