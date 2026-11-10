package com.android.core.commonui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.core.commonui.R
import com.android.core.commonui.theme.MonitoringDevicesTheme
import kotlin.math.roundToInt

/**
 * Full width, the caption row above it.
 * [progress] in `0f..1f` (clamped, changes are animated); `null` means unknown progress (indeterminate).
 * [showPercent] is ignored while progress is unknown.
 */
@Composable
fun ProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    label: String? = null,
    showPercent: Boolean = false
) {
    val fraction = progress?.coerceIn(0f, 1f)
    val percent = if (showPercent && fraction != null) (fraction * 100).roundToInt() else null

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null || percent != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (percent != null) {
                    Text(
                        text = stringResource(R.string.common_ui_percent, percent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (fraction == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress"
            )
            LinearProgressIndicator(progress = { animatedFraction }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressBarPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressBar(progress = null, label = "Searching for devices")
            ProgressBar(progress = 0.65f, label = "Downloading log", showPercent = true)
            ProgressBar(progress = 0.3f)
        }
    }
}
