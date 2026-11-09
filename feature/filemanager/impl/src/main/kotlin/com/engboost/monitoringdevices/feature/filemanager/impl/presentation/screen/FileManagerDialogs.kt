package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.screen

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.engboost.monitoringdevices.feature.filemanager.impl.R
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.DeletionUi

@Composable
internal fun DeleteConfirmationDialog(
    deletion: DeletionUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = deletion.title) },
        text = { Text(text = deletion.text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.fm_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.fm_cancel))
            }
        }
    )
}

@Composable
internal fun CreateDirectoryDialog(
    error: String?,
    isBusy: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.fm_new_folder)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { value -> name = value },
                label = { Text(text = stringResource(R.string.fm_folder_name)) },
                singleLine = true,
                isError = error != null,
                supportingText = { error?.let { message -> Text(text = message) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!isBusy) onConfirm(name) }),
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = !isBusy) {
                Text(text = stringResource(R.string.fm_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.fm_cancel))
            }
        }
    )
}
