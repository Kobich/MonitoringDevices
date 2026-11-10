package com.android.core.commonui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.core.commonui.theme.MonitoringDevicesTheme

/** Fills the screen. */
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier,
    text: String? = null
) {
    CenteredContent(modifier = modifier) {
        Spinner()
        text?.let { caption -> Text(text = caption, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingContentPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        LoadingContent(text = "Rendering document…")
    }
}
