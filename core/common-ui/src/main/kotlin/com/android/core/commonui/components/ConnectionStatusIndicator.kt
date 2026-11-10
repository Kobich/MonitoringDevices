package com.android.core.commonui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.core.commonui.R
import com.android.core.commonui.icons.AppIcons
import com.android.core.commonui.theme.MonitoringDevicesTheme
import com.android.core.commonui.theme.StatusColorRole

/** Features map their own connection state to it. */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Pill with a status icon and a caption.
 *
 * Soft status colors: green when connected, amber while connecting, muted red when disconnected.
 * The icon tells it apart without colors too: link, spinner, crossed-out link.
 */
@Composable
fun ConnectionStatusIndicator(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    label: String = connectionStatusLabel(status),
    iconSize: Dp = 18.dp
) {
    val role = connectionStatusRole(status)
    val containerColor by animateColorAsState(targetValue = role.container, label = "statusContainer")
    val contentColor by animateColorAsState(targetValue = role.onContainer, label = "statusContent")

    Surface(shape = CircleShape, color = containerColor, contentColor = contentColor, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            ConnectionStatusIcon(status = status, size = iconSize, contentDescription = null)
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * For list rows and app bars.
 * Pass `contentDescription = null` when a caption with the status is next to it.
 */
@Composable
fun ConnectionStatusIcon(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = connectionStatusRole(status).accent,
    contentDescription: String? = connectionStatusLabel(status)
) {
    val animatedTint by animateColorAsState(targetValue = tint, label = "statusTint")

    Crossfade(
        targetState = status,
        label = "statusIcon",
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
    ) { shown ->
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            when (shown) {
                // fillMaxSize: on its own an Icon keeps the vector's 24 dp and ignores a larger [size].
                ConnectionStatus.CONNECTED -> Icon(
                    imageVector = AppIcons.Link,
                    contentDescription = null,
                    tint = animatedTint,
                    modifier = Modifier.fillMaxSize()
                )

                ConnectionStatus.CONNECTING -> SpinnerArc(
                    color = animatedTint,
                    strokeWidth = (size / 12).coerceAtLeast(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * 0.1f)
                )

                ConnectionStatus.DISCONNECTED -> Icon(
                    imageVector = AppIcons.LinkOff,
                    contentDescription = null,
                    tint = animatedTint,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun connectionStatusLabel(status: ConnectionStatus): String = stringResource(
    when (status) {
        ConnectionStatus.DISCONNECTED -> R.string.common_ui_connection_disconnected
        ConnectionStatus.CONNECTING -> R.string.common_ui_connection_connecting
        ConnectionStatus.CONNECTED -> R.string.common_ui_connection_connected
    }
)

@Composable
internal fun connectionStatusRole(status: ConnectionStatus): StatusColorRole {
    val colors = MonitoringDevicesTheme.statusColors
    return when (status) {
        ConnectionStatus.CONNECTED -> colors.positive
        ConnectionStatus.CONNECTING -> colors.pending
        ConnectionStatus.DISCONNECTED -> colors.negative
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionStatusIndicatorPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        ConnectionStatusPreviewContent()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141218)
@Composable
private fun ConnectionStatusIndicatorDarkPreview() {
    MonitoringDevicesTheme(darkTheme = true, dynamicColor = false) {
        ConnectionStatusPreviewContent()
    }
}

@Composable
private fun ConnectionStatusPreviewContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ConnectionStatus.entries.forEach { status -> ConnectionStatusIndicator(status = status) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ConnectionStatus.entries.forEach { status -> ConnectionStatusIcon(status = status, size = 24.dp) }
        }
    }
}
