package com.android.core.commonui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.core.commonui.components.BackTopAppBar
import com.android.core.commonui.components.ChoiceDialog
import com.android.core.commonui.components.ChoiceDialogUi
import com.android.core.commonui.components.ConfirmDialog
import com.android.core.commonui.components.ConnectionStatus
import com.android.core.commonui.components.ConnectionStatusIndicator
import com.android.core.commonui.components.DialogChoice
import com.android.core.commonui.components.ProgressBar
import com.android.core.commonui.icons.AppIcons
import com.android.core.commonui.icons.FileKind
import com.android.core.commonui.icons.FileTypeIcon
import com.android.core.commonui.theme.MonitoringDevicesTheme

/**
 * Everything common-ui offers on one screen: dialogs (opened by buttons), components and icons.
 * A new component or [AppIcons] entry goes here too. Texts are samples, not app strings.
 */
@Composable
fun UiCatalogScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var openDialog by rememberSaveable { mutableStateOf<CatalogDialog?>(null) }
    var lastAnswer by rememberSaveable { mutableStateOf<String?>(null) }

    openDialog?.let { dialog ->
        CatalogDialogContent(
            dialog = dialog,
            onNext = { next -> openDialog = next },
            onAnswer = { answer ->
                lastAnswer = answer
                openDialog = null
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = "UI catalog", onNavigationClick = onBackClick) }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            header("Dialogs")
            items(
                items = CatalogDialog.entries.filter { dialog -> dialog.isEntry },
                key = { dialog -> "dialog-${dialog.name}" },
                span = { GridItemSpan(maxLineSpan) }
            ) { dialog ->
                OutlinedButton(onClick = { openDialog = dialog }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = dialog.title)
                }
            }
            fullLineItem {
                Text(
                    text = "Last answer: ${lastAnswer ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            header("Components")
            fullLineItem { ProgressBar(progress = null, label = "ProgressBar, unknown progress") }
            fullLineItem { ProgressBar(progress = 0.42f, label = "ProgressBar with percent", showPercent = true) }
            items(ConnectionStatus.entries, key = { "status-${it.name}" }, span = { GridItemSpan(maxLineSpan) }) { status ->
                ConnectionStatusIndicator(status = status)
            }

            header("AppIcons")
            items(appIcons, key = { (name, _) -> "app-$name" }) { (name, icon) ->
                IconTile(name = name) { Icon(imageVector = icon, contentDescription = null) }
            }

            header("FileTypeIcons")
            items(FileKind.entries, key = { kind -> "file-${kind.name}" }) { kind ->
                IconTile(name = kind.name) { FileTypeIcon(kind = kind) }
            }
        }
    }
}

/** [isEntry] = `false`: a later step of the chain, reached only from another dialog. */
private enum class CatalogDialog(val title: String, val isEntry: Boolean = true) {
    CONFIRM("ConfirmDialog"),
    DESTRUCTIVE("ConfirmDialog, destructive"),
    INPUT("ConfirmDialog with a text field"),
    PROGRESS("ConfirmDialog with progress, not dismissible"),
    CHOICE("ChoiceDialog, three choices"),
    CHAIN_DELETE("Dialog chain: delete, then keep a copy?"),
    CHAIN_KEEP_COPY("Keep a copy?", isEntry = false)
}

private enum class KeepCopyChoice { KEEP, DELETE, BACK }

@Composable
private fun CatalogDialogContent(
    dialog: CatalogDialog,
    onNext: (CatalogDialog) -> Unit,
    onAnswer: (String) -> Unit
) {
    val onDismiss = { onAnswer("dismissed") }
    when (dialog) {
        CatalogDialog.CONFIRM -> ConfirmDialog(
            title = "Leave without saving?",
            confirmLabel = "Leave",
            dismissLabel = "Stay",
            onConfirm = { onAnswer("Leave") },
            onDismiss = onDismiss
        ) {
            Text(text = "Changes on this screen will be lost.")
        }

        CatalogDialog.DESTRUCTIVE -> ConfirmDialog(
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

        CatalogDialog.INPUT -> {
            var name by remember { mutableStateOf("") }
            ConfirmDialog(
                title = "New folder",
                confirmLabel = "Create",
                dismissLabel = "Cancel",
                confirmEnabled = name.isNotBlank(),
                onConfirm = { onAnswer("Create \"$name\"") },
                onDismiss = onDismiss
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(text = "Name") }, singleLine = true)
            }
        }

        CatalogDialog.PROGRESS -> ConfirmDialog(
            title = "Copying files",
            confirmLabel = "Hide",
            dismissible = false,
            onConfirm = { onAnswer("Hide") },
            onDismiss = onDismiss
        ) {
            ProgressBar(progress = 0.42f, label = "Report.pdf", showPercent = true)
        }

        CatalogDialog.CHOICE -> ChoiceDialog(
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

        // A chain: the answer decides the next dialog. In a feature this `when` lives in the ViewModel.
        CatalogDialog.CHAIN_DELETE -> ConfirmDialog(
            title = "Delete \"Report.pdf\"?",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            destructive = true,
            onConfirm = { onNext(CatalogDialog.CHAIN_KEEP_COPY) },
            onDismiss = onDismiss
        )

        CatalogDialog.CHAIN_KEEP_COPY -> ChoiceDialog(
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
                    KeepCopyChoice.BACK -> onNext(CatalogDialog.CHAIN_DELETE)
                }
            },
            onDismiss = onDismiss
        )
    }
}

private val appIcons: List<Pair<String, ImageVector>>
    get() = listOf(
        "Back" to AppIcons.Back,
        "Close" to AppIcons.Close,
        "Delete" to AppIcons.Delete,
        "More" to AppIcons.More,
        "Add" to AppIcons.Add,
        "Check" to AppIcons.Check,
        "Play" to AppIcons.Play,
        "Pause" to AppIcons.Pause,
        "Folder" to AppIcons.Folder,
        "File" to AppIcons.File,
        "Link" to AppIcons.Link,
        "LinkOff" to AppIcons.LinkOff
    )

private fun LazyGridScope.header(title: String) {
    fullLineItem { Text(text = title, style = MaterialTheme.typography.titleMedium) }
}

private fun LazyGridScope.fullLineItem(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun IconTile(name: String, icon: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UiCatalogScreenPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        UiCatalogScreen(onBackClick = {})
    }
}
