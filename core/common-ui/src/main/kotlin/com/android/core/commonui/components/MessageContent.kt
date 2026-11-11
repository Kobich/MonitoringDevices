package com.android.core.commonui.components

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.android.core.commonui.theme.AppTheme

/** Full-screen empty or error state. */
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
    AppTheme(dynamicColor = false) {
        MessageContent(title = "Can't open the file", text = "The file is damaged", actionLabel = "Retry")
    }
}
