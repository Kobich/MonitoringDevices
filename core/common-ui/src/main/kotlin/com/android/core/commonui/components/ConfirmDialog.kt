package com.android.core.commonui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.android.core.commonui.icons.AppIcons
import com.android.core.commonui.theme.MonitoringDevicesTheme

/**
 * Dialog with a title, a body in [content] and a confirm button; [dismissLabel] adds a second button.
 * [destructive] paints the confirm button with the error color (delete, disconnect, reset).
 * [dismissible] = `false` ignores back and taps outside: the user has to press a button.
 */
@Composable
fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    dismissible: Boolean = true,
    content: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = content,
        icon = icon?.let { image -> { Icon(imageVector = image, contentDescription = null) } },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(
                    text = confirmLabel,
                    color = if (destructive && confirmEnabled) MaterialTheme.colorScheme.error else Color.Unspecified
                )
            }
        },
        dismissButton = dismissLabel?.let { label ->
            {
                TextButton(onClick = onDismiss) {
                    Text(text = label)
                }
            }
        },
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible),
        modifier = modifier
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        ConfirmDialog(
            title = "Remove device?",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            icon = AppIcons.Delete,
            destructive = true,
            onConfirm = {},
            onDismiss = {}
        ) {
            Text(text = "The device will disappear from the list. You can add it again later.")
        }
    }
}

@Preview
@Composable
private fun ConfirmDialogProgressPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        ConfirmDialog(
            title = "Updating firmware",
            confirmLabel = "Hide",
            dismissible = false,
            onConfirm = {},
            onDismiss = {}
        ) {
            ProgressBar(progress = 0.42f, label = "Uploading", showPercent = true)
        }
    }
}
