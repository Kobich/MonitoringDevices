package com.android.core.commonui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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

/** UI state of a connection; features map their own connection state to it. */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Pill with a status icon and a caption. [label] overrides the default caption of the [status].
 *
 * The status reads from the shape, not only the color: connected is a filled pill with a link,
 * connecting is a spinner, disconnected is an empty outlined pill with a crossed-out link.
 */
@Composable
fun ConnectionStatusIndicator(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    label: String = connectionStatusLabel(status)
) {
    val colors = connectionStatusColors(status)
    val containerColor by animateColorAsState(targetValue = colors.container, label = "statusContainer")
    val contentColor by animateColorAsState(targetValue = colors.content, label = "statusContent")
    val borderColor by animateColorAsState(targetValue = colors.border, label = "statusBorder")

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(width = 1.dp, color = borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            ConnectionStatusIcon(status = status, contentDescription = null)
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Only the icon, for list rows and app bars: link, spinner, crossed-out link.
 * Pass `contentDescription = null` when a caption with the status is next to it.
 */
@Composable
fun ConnectionStatusIcon(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = connectionStatusLabel(status)
) {
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
                ConnectionStatus.CONNECTED -> Icon(imageVector = AppIcons.Link, contentDescription = null, tint = tint)
                ConnectionStatus.CONNECTING -> CircularProgressIndicator(
                    color = tint,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * 0.1f)
                )

                ConnectionStatus.DISCONNECTED -> Icon(imageVector = AppIcons.LinkOff, contentDescription = null, tint = tint)
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

private class StatusColors(val container: Color, val content: Color, val border: Color)

@Composable
private fun connectionStatusColors(status: ConnectionStatus): StatusColors {
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        ConnectionStatus.CONNECTED -> StatusColors(
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            border = Color.Transparent
        )

        ConnectionStatus.CONNECTING -> StatusColors(
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
            border = Color.Transparent
        )

        ConnectionStatus.DISCONNECTED -> StatusColors(
            container = Color.Transparent,
            content = scheme.onSurfaceVariant,
            border = scheme.outline
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionStatusIndicatorPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConnectionStatus.entries.forEach { status -> ConnectionStatusIndicator(status = status) }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ConnectionStatus.entries.forEach { status -> ConnectionStatusIcon(status = status, size = 24.dp) }
            }
        }
    }
}
