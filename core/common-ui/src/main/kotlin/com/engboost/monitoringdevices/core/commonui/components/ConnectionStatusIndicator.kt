package com.engboost.monitoringdevices.core.commonui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.core.commonui.R
import com.engboost.monitoringdevices.core.commonui.theme.MonitoringDevicesTheme

/** UI state of a connection; features map their own connection state to it. */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/** Pill with a status dot and a caption. [label] overrides the default caption of the [status]. */
@Composable
fun ConnectionStatusIndicator(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    label: String = connectionStatusLabel(status)
) {
    val colors = connectionStatusColors(status)
    val containerColor by animateColorAsState(targetValue = colors.container, label = "statusContainer")
    val contentColor by animateColorAsState(targetValue = colors.content, label = "statusContent")

    Surface(shape = CircleShape, color = containerColor, contentColor = contentColor, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            ConnectionStatusDot(status = status)
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Only the dot, for list rows and app bars. Decorative: put the status into text or semantics next to it. */
@Composable
fun ConnectionStatusDot(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val color by animateColorAsState(targetValue = connectionStatusColors(status).dot, label = "statusDot")
    val alpha = if (status == ConnectionStatus.CONNECTING) {
        val transition = rememberInfiniteTransition(label = "connecting")
        val pulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 700), repeatMode = RepeatMode.Reverse),
            label = "connectingPulse"
        )
        pulse
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(size)
            .alpha(alpha)
            .background(color = color, shape = CircleShape)
    )
}

@Composable
fun connectionStatusLabel(status: ConnectionStatus): String = stringResource(
    when (status) {
        ConnectionStatus.DISCONNECTED -> R.string.common_ui_connection_disconnected
        ConnectionStatus.CONNECTING -> R.string.common_ui_connection_connecting
        ConnectionStatus.CONNECTED -> R.string.common_ui_connection_connected
    }
)

private class StatusColors(val container: Color, val content: Color, val dot: Color)

@Composable
private fun connectionStatusColors(status: ConnectionStatus): StatusColors {
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        ConnectionStatus.CONNECTED -> StatusColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary)
        ConnectionStatus.CONNECTING -> StatusColors(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary)
        ConnectionStatus.DISCONNECTED -> StatusColors(scheme.errorContainer, scheme.onErrorContainer, scheme.error)
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionStatusIndicatorPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConnectionStatus.entries.forEach { status -> ConnectionStatusIndicator(status = status) }
        }
    }
}
