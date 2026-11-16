package com.android.core.commonui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.android.core.commonui.theme.AppTheme

/** A [ConfirmDialog] with a hidden password field; confirming is possible once something is typed. */
@Composable
fun PasswordDialog(
    title: String,
    passwordLabel: String,
    confirmLabel: String,
    dismissLabel: String,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Not rememberSaveable: the password must not go into the saved instance state.
    var password by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ConfirmDialog(
        title = title,
        confirmLabel = confirmLabel,
        dismissLabel = dismissLabel,
        confirmEnabled = password.isNotEmpty(),
        onConfirm = { onConfirm(password) },
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = { value -> password = value },
            label = { Text(text = passwordLabel) },
            singleLine = true,
            isError = error != null,
            supportingText = { error?.let { message -> Text(text = message) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (password.isNotEmpty()) onConfirm(password) }),
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordDialogPreview() {
    AppTheme(dynamicColor = false) {
        PasswordDialog(
            title = "Password to export",
            passwordLabel = "Password",
            confirmLabel = "OK",
            dismissLabel = "Cancel",
            error = "Wrong password",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
