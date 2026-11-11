package com.android.core.commonui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import com.android.core.commonui.theme.AppTheme

/**
 * One cycle: the arc grows, then shrinks. Material's indeterminate indicator does the same in about
 * 1.3 s and has no speed setting, so the motion is redone on its determinate indicator.
 */
private const val CYCLE_MILLIS = 2000
private const val MIN_SWEEP = 0.1f
private const val MAX_SWEEP = 0.75f

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
 * Material's indeterminate motion at [CYCLE_MILLIS] per cycle. Fills the size given by [modifier].
 * Screen readers hear an indeterminate progress, not the arc's current length.
 */
@Composable
internal fun SpinnerArc(
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    val cycle = rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = CYCLE_MILLIS, easing = LinearEasing)),
        label = "spinnerCycle"
    )
    Box(modifier = modifier.clearAndSetSemantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        CircularProgressIndicator(
            progress = { spinnerSweep(cycle.value) },
            color = color,
            strokeWidth = strokeWidth,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
            // Both read the cycle while drawing, so the spinner redraws without recomposing.
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = spinnerTailDegrees(cycle.value) }
        )
    }
}

/**
 * Like Material's indicator, the head runs ahead while the tail stands, then the tail catches up while
 * the head stands. The whole arc also turns, by just enough that the tail goes round exactly once per
 * cycle: the next cycle starts where this one ended. [cycle] is in `0..1`.
 */
internal fun spinnerSweep(cycle: Float): Float {
    val growth = MAX_SWEEP - MIN_SWEEP
    return if (cycle < 0.5f) {
        MIN_SWEEP + growth * FastOutSlowInEasing.transform(cycle * 2f)
    } else {
        MAX_SWEEP - growth * FastOutSlowInEasing.transform(cycle * 2f - 1f)
    }
}

/** Where the tail of [spinnerSweep]'s arc is, clockwise from 12 o'clock. */
internal fun spinnerTailDegrees(cycle: Float): Float {
    val growth = MAX_SWEEP - MIN_SWEEP
    val turn = 360f * (1f - growth) * cycle
    if (cycle < 0.5f) return turn
    return turn + 360f * growth * FastOutSlowInEasing.transform(cycle * 2f - 1f)
}

@Preview(showBackground = true)
@Composable
private fun SpinnerPreview() {
    AppTheme(dynamicColor = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spinner()
            Spinner(compact = true)
        }
    }
}
