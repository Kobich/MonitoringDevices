package com.android.core.commonui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.android.core.commonui.theme.MonitoringDevicesTheme

/**
 * A dialog described as data, so a ViewModel can keep it in its state. For a chain of dialogs the
 * ViewModel keeps the current step and, when a choice comes back, puts the next step (or `null`) there.
 *
 * @property choices buttons in the order they are shown; see [ChoiceDialog] for the layout.
 * @property text optional body under the title.
 */
@Immutable
data class ChoiceDialogUi<out T>(
    val title: String,
    val choices: List<DialogChoice<T>>,
    val text: String? = null,
    val icon: ImageVector? = null,
    /** `false` ignores back and taps outside: the user has to pick a choice. */
    val dismissible: Boolean = true
)

/**
 * One button of a [ChoiceDialog].
 *
 * @property value what [ChoiceDialog] reports when this button is pressed.
 * @property destructive paints the label with the error color (delete, reset).
 */
@Immutable
data class DialogChoice<out T>(
    val value: T,
    val label: String,
    val destructive: Boolean = false
)

/** Two choices stand side by side, the last one on the right; three and more are stacked. */
@Composable
fun <T> ChoiceDialog(
    dialog: ChoiceDialogUi<T>,
    onChoice: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val choices = dialog.choices
    val isStacked = choices.size > 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialog.title) },
        text = dialog.text?.let { text -> { Text(text = text) } },
        icon = dialog.icon?.let { image -> { Icon(imageVector = image, contentDescription = null) } },
        confirmButton = {
            if (isStacked) {
                Column(horizontalAlignment = Alignment.End) {
                    choices.forEach { choice -> ChoiceButton(choice, onChoice) }
                }
            } else {
                choices.lastOrNull()?.let { choice -> ChoiceButton(choice, onChoice) }
            }
        },
        dismissButton = if (!isStacked && choices.size == 2) {
            { ChoiceButton(choices.first(), onChoice) }
        } else {
            null
        },
        properties = DialogProperties(dismissOnBackPress = dialog.dismissible, dismissOnClickOutside = dialog.dismissible),
        modifier = modifier
    )
}

@Composable
private fun <T> ChoiceButton(choice: DialogChoice<T>, onChoice: (T) -> Unit) {
    TextButton(onClick = { onChoice(choice.value) }) {
        Text(
            text = choice.label,
            color = if (choice.destructive) MaterialTheme.colorScheme.error else Color.Unspecified
        )
    }
}

@Preview
@Composable
private fun ChoiceDialogPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        ChoiceDialog(
            dialog = ChoiceDialogUi(
                title = "Keep a copy?",
                text = "The file is deleted from the app. A copy can stay in Downloads.",
                choices = listOf(
                    DialogChoice(value = 1, label = "Keep a copy"),
                    DialogChoice(value = 2, label = "Delete for good", destructive = true),
                    DialogChoice(value = 3, label = "Back")
                )
            ),
            onChoice = {},
            onDismiss = {}
        )
    }
}
