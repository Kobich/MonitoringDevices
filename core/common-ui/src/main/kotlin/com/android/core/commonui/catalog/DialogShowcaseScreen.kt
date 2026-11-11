package com.android.core.commonui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.core.commonui.components.BackTopAppBar
import com.android.core.commonui.components.ChoiceDialog
import com.android.core.commonui.components.ChoiceDialogUi
import com.android.core.commonui.components.ConfirmDialog
import com.android.core.commonui.components.DialogChoice
import com.android.core.commonui.components.ProgressBar
import com.android.core.commonui.icons.AppIcons
import com.android.core.commonui.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * Every kind of window the app shows: dialogs, alerts, a dialog chain, snackbars and a bottom sheet.
 * The answer of the last window is shown at the bottom. Texts are samples, not app strings.
 */
@Composable
fun DialogShowcaseScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var openWindow by rememberSaveable { mutableStateOf<ShowcaseWindow?>(null) }
    var lastAnswer by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    openWindow?.let { window ->
        ShowcaseWindowContent(
            window = window,
            onNext = { next -> openWindow = next },
            onAnswer = { answer ->
                lastAnswer = answer
                openWindow = null
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = "Dialogs and windows", onNavigationClick = onBackClick) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShowcaseSection.entries.forEach { section ->
                item(key = "section-${section.name}") { SectionTitle(section.title) }
                items(
                    items = ShowcaseWindow.entries.filter { window -> window.section == section && window.isEntry },
                    key = { window -> "window-${window.name}" }
                ) { window ->
                    ShowcaseButton(text = window.title, onClick = { openWindow = window })
                }
            }

            item(key = "section-snackbars") { SectionTitle("Snackbars") }
            item(key = "snackbar") {
                ShowcaseButton(text = "Message") {
                    scope.launch { snackbarHostState.showSnackbar("Folder created") }
                }
            }
            item(key = "snackbar-action") {
                ShowcaseButton(text = "Message with an action") {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar("Deleted \"Report.pdf\"", actionLabel = "Undo")
                        lastAnswer = if (result == SnackbarResult.ActionPerformed) "Undo" else "Snackbar closed"
                    }
                }
            }

            item(key = "answer") {
                Text(
                    text = "Last answer: ${lastAnswer ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private enum class ShowcaseSection(val title: String) {
    DIALOGS("Dialogs"),
    ALERTS("Alerts"),
    CHAIN("Dialog chain"),
    SHEETS("Bottom sheet")
}

/** [isEntry] = `false`: a later step of the chain, opened only from another window. */
private enum class ShowcaseWindow(val title: String, val section: ShowcaseSection, val isEntry: Boolean = true) {
    CONFIRM("Confirm", ShowcaseSection.DIALOGS),
    DELETE("Delete, destructive", ShowcaseSection.DIALOGS),
    INPUT("Text field", ShowcaseSection.DIALOGS),
    CHOICE("Three choices", ShowcaseSection.DIALOGS),
    INFO("Information, one button", ShowcaseSection.ALERTS),
    PROGRESS("Progress, can't be dismissed", ShowcaseSection.ALERTS),
    CHAIN_DELETE("Delete, then keep a copy?", ShowcaseSection.CHAIN),
    CHAIN_KEEP_COPY("Keep a copy?", ShowcaseSection.CHAIN, isEntry = false),
    DETAILS_SHEET("Details", ShowcaseSection.SHEETS)
}

private enum class KeepCopyChoice { KEEP, DELETE, BACK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowcaseWindowContent(
    window: ShowcaseWindow,
    onNext: (ShowcaseWindow) -> Unit,
    onAnswer: (String) -> Unit
) {
    val onDismiss = { onAnswer("Dismissed") }
    when (window) {
        ShowcaseWindow.CONFIRM -> ConfirmDialog(
            title = "Leave without saving?",
            confirmLabel = "Leave",
            dismissLabel = "Stay",
            onConfirm = { onAnswer("Leave") },
            onDismiss = onDismiss
        ) {
            Text(text = "Changes on this screen will be lost.")
        }

        ShowcaseWindow.DELETE -> ConfirmDialog(
            title = "Delete \"Report.pdf\"?",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            icon = AppIcons.Delete,
            destructive = true,
            onConfirm = { onAnswer("Delete") },
            onDismiss = onDismiss
        ) {
            Text(text = "The file will be deleted permanently.")
        }

        ShowcaseWindow.INPUT -> {
            var name by remember { mutableStateOf("") }
            ConfirmDialog(
                title = "New folder",
                confirmLabel = "Create",
                dismissLabel = "Cancel",
                confirmEnabled = name.isNotBlank(),
                onConfirm = { onAnswer("Create \"$name\"") },
                onDismiss = onDismiss
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { value -> name = value },
                    label = { Text(text = "Name") },
                    singleLine = true
                )
            }
        }

        ShowcaseWindow.CHOICE -> ChoiceDialog(
            dialog = ChoiceDialogUi(
                title = "Open \"Track.mp4\" as",
                choices = listOf(
                    DialogChoice(value = "Video", label = "Video"),
                    DialogChoice(value = "Audio only", label = "Audio only"),
                    DialogChoice(value = "Cancel", label = "Cancel")
                )
            ),
            onChoice = onAnswer,
            onDismiss = onDismiss
        )

        ShowcaseWindow.INFO -> ConfirmDialog(
            title = "Export finished",
            confirmLabel = "OK",
            onConfirm = { onAnswer("OK") },
            onDismiss = onDismiss
        ) {
            Text(text = "3 files were copied to Downloads.")
        }

        ShowcaseWindow.PROGRESS -> ConfirmDialog(
            title = "Copying files",
            confirmLabel = "Hide",
            dismissible = false,
            onConfirm = { onAnswer("Hide") },
            onDismiss = onDismiss
        ) {
            ProgressBar(progress = 0.42f, label = "Report.pdf", showPercent = true)
        }

        // The answer decides the next window. In a feature this `when` lives in the ViewModel.
        ShowcaseWindow.CHAIN_DELETE -> ConfirmDialog(
            title = "Delete \"Report.pdf\"?",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            destructive = true,
            onConfirm = { onNext(ShowcaseWindow.CHAIN_KEEP_COPY) },
            onDismiss = onDismiss
        )

        ShowcaseWindow.CHAIN_KEEP_COPY -> ChoiceDialog(
            dialog = ChoiceDialogUi(
                title = "Keep a copy?",
                text = "The file is deleted from the app. A copy can stay in Downloads.",
                choices = listOf(
                    DialogChoice(KeepCopyChoice.KEEP, "Keep a copy"),
                    DialogChoice(KeepCopyChoice.DELETE, "Delete for good", destructive = true),
                    DialogChoice(KeepCopyChoice.BACK, "Back")
                )
            ),
            onChoice = { choice ->
                when (choice) {
                    KeepCopyChoice.KEEP -> onAnswer("Deleted, copy kept")
                    KeepCopyChoice.DELETE -> onAnswer("Deleted for good")
                    KeepCopyChoice.BACK -> onNext(ShowcaseWindow.CHAIN_DELETE)
                }
            },
            onDismiss = onDismiss
        )

        ShowcaseWindow.DETAILS_SHEET -> ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Office Wi-Fi", style = MaterialTheme.typography.titleLarge)
                Text(text = "Signal: good · 5 GHz · WPA3")
                Text(text = "BSSID: 00:11:22:33:44:55", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ShowcaseButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogShowcaseScreenPreview() {
    AppTheme(dynamicColor = false) {
        DialogShowcaseScreen(onBackClick = {})
    }
}
