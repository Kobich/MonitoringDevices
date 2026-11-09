package com.android.appmanager.feature.documentviewer.presentation.screen

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.android.appmanager.feature.documentviewer.R
import com.android.core.commonui.components.ConfirmDialog

@Composable
internal fun PasswordDialog(
    isWrongPassword: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }

    ConfirmDialog(
        title = stringResource(R.string.viewer_password_title),
        confirmLabel = stringResource(R.string.viewer_open),
        dismissLabel = stringResource(R.string.viewer_cancel),
        confirmEnabled = password.isNotEmpty(),
        onConfirm = { onSubmit(password) },
        onDismiss = onDismiss
    ) {
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
    }
}
