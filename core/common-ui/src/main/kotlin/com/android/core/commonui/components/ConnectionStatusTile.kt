package com.android.core.commonui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.core.commonui.theme.MonitoringDevicesTheme

/**
 * Large status: the icon in a soft colored circle, the caption under it.
 * For a device card or the empty area of a screen; [ConnectionStatusIndicator] is the compact pill.
 */
@Composable
fun ConnectionStatusTile(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    label: String = connectionStatusLabel(status),
    iconSize: Dp = 36.dp
) {
    val circleColor by animateColorAsState(targetValue = connectionStatusRole(status).container, label = "tileCircle")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconSize * 2)
                .background(color = circleColor, shape = CircleShape)
        ) {
            ConnectionStatusIcon(status = status, size = iconSize, contentDescription = null)
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionStatusTilePreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        ConnectionStatusTileRow()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141218)
@Composable
private fun ConnectionStatusTileDarkPreview() {
    MonitoringDevicesTheme(darkTheme = true, dynamicColor = false) {
        ConnectionStatusTileRow()
    }
}

@Composable
private fun ConnectionStatusTileRow() {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ConnectionStatus.entries.forEach { status -> ConnectionStatusTile(status = status) }
    }
}
