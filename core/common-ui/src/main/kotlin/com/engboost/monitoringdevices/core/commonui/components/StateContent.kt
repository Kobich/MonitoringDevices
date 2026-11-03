package com.engboost.monitoringdevices.core.commonui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.core.commonui.theme.MonitoringDevicesTheme

/** Fills the available space and centers [content] as a column. */
@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Full-screen progress with an optional caption. */
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier,
    text: String? = null
) {
    CenteredContent(modifier = modifier) {
        CircularProgressIndicator()
        text?.let { caption -> Text(text = caption, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** Empty, error or info state: title, optional details and an optional action button. */
@Composable
fun MessageContent(
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {}
) {
    CenteredContent(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        text?.let { details ->
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        actionLabel?.let { label ->
            Button(onClick = onActionClick) {
                Text(text = label)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageContentPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        MessageContent(title = "Can't open the file", text = "The file is damaged", actionLabel = "Retry")
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingContentPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        LoadingContent(text = "Rendering document…")
    }
}
