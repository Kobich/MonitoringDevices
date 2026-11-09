package com.android.appmanager.feature.viewer.impl.presentation.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.android.appmanager.feature.viewer.impl.R

@Composable
internal fun PasswordDialog(
    isWrongPassword: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.viewer_password_title)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { value -> password = value },
                label = { Text(text = stringResource(R.string.viewer_password_label)) },
                singleLine = true,
                isError = isWrongPassword,
                supportingText = {
                    if (isWrongPassword) Text(text = stringResource(R.string.viewer_password_wrong))
                },
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) {
                Text(text = stringResource(R.string.viewer_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.viewer_cancel))
            }
        }
    )
}
