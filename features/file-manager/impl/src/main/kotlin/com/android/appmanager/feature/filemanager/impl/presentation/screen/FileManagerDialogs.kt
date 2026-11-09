package com.android.appmanager.feature.filemanager.impl.presentation.screen

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.android.appmanager.feature.filemanager.impl.R
import com.android.appmanager.feature.filemanager.impl.presentation.model.DeletionUi
import com.android.core.commonui.components.ConfirmDialog

@Composable
internal fun DeleteConfirmationDialog(
    deletion: DeletionUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        title = deletion.title,
        confirmLabel = stringResource(R.string.fm_delete),
        dismissLabel = stringResource(R.string.fm_cancel),
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    ) {
        Text(text = deletion.text)
    }
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

    ConfirmDialog(
        title = stringResource(R.string.fm_new_folder),
        confirmLabel = stringResource(R.string.fm_create),
        dismissLabel = stringResource(R.string.fm_cancel),
        confirmEnabled = !isBusy,
        onConfirm = { onConfirm(name) },
        onDismiss = onDismiss
    ) {
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
    }
}
