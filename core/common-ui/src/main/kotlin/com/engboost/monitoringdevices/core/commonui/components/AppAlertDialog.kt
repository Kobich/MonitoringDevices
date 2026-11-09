package com.engboost.monitoringdevices.core.commonui.components

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
import com.engboost.monitoringdevices.core.commonui.icons.AppIcons
import com.engboost.monitoringdevices.core.commonui.theme.MonitoringDevicesTheme

/**
 * Dialog with a title, a body and one or two text buttons.
 * [destructive] paints the confirm button with the error color (delete, disconnect, reset).
 * [dismissible] = `false` ignores back and taps outside: the user has to press a button.
 */
@Composable
fun AppAlertDialog(
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

/** [AppAlertDialog] with plain [text] as the body. */
@Composable
fun AppAlertDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    dismissible: Boolean = true
) {
    AppAlertDialog(
        title = title,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        dismissLabel = dismissLabel,
        icon = icon,
        destructive = destructive,
        confirmEnabled = confirmEnabled,
        dismissible = dismissible,
        content = { Text(text = text) }
    )
}

@Preview
@Composable
private fun AppAlertDialogPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        AppAlertDialog(
            title = "Remove device?",
            text = "The device will disappear from the list. You can add it again later.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            icon = AppIcons.Delete,
            destructive = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun AppAlertDialogProgressPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        AppAlertDialog(
            title = "Updating firmware",
            confirmLabel = "Hide",
            dismissible = false,
            onConfirm = {},
            onDismiss = {}
        ) {
            AppProgressBar(progress = 0.42f, label = "Uploading", showPercent = true)
        }
    }
}
