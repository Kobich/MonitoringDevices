package com.engboost.monitoringdevices.core.commonui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.core.commonui.icons.AppIcons
import com.engboost.monitoringdevices.core.commonui.theme.MonitoringDevicesTheme

/** Display only: the row handles clicks. */
@Composable
fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val colors = MaterialTheme.colorScheme
    val fillColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surface,
        label = "selectionFill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outline,
        label = "selectionBorder"
    )
    val checkScale by animateFloatAsState(targetValue = if (selected) 1f else 0f, label = "selectionCheck")

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fillColor)
            .border(width = 2.dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checkScale > 0f) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier
                    .size(size * 0.7f)
                    .scale(checkScale)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectionIndicatorPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        Row(modifier = Modifier.padding(8.dp)) {
            SelectionIndicator(selected = false, modifier = Modifier.padding(4.dp))
            SelectionIndicator(selected = true, modifier = Modifier.padding(4.dp))
        }
    }
}
