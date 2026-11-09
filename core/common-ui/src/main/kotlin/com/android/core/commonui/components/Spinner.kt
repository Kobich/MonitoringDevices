package com.android.core.commonui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.core.commonui.theme.MonitoringDevicesTheme

/**
 * Indeterminate circular progress. [compact] is the small one for a place inside content
 * (a page, a picture, a button); the default size is for an empty area.
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (compact) {
        CircularProgressIndicator(modifier = modifier.size(24.dp), strokeWidth = 2.dp)
    } else {
        CircularProgressIndicator(modifier = modifier)
    }
}

@Preview(showBackground = true)
@Composable
private fun SpinnerPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spinner()
            Spinner(compact = true)
        }
    }
}
