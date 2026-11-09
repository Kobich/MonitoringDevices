package com.android.core.commonui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.android.core.commonui.components.ConfirmDialog
import com.android.core.commonui.components.ConnectionStatus
import com.android.core.commonui.components.ConnectionStatusIndicator
import com.android.core.commonui.components.LoadingContent
import com.android.core.commonui.components.ProgressBar
import com.android.core.commonui.icons.AppIcons
import com.android.core.commonui.icons.FileKind
import com.android.core.commonui.icons.FileTypeIcon
import com.android.core.commonui.theme.MonitoringDevicesTheme
import kotlinx.coroutines.delay

/**
 * Components and icons of common-ui on one screen. A new component or [AppIcons] entry goes here too.
 * All kinds of dialogs and other windows are on [DialogShowcaseScreen]. Texts are samples, not app strings.
 */
@Composable
fun UiCatalogScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isDeleteDialogOpen) {
        ConfirmDialog(
            title = "Delete \"Report.pdf\"?",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            destructive = true,
            onConfirm = { isDeleteDialogOpen = false },
            onDismiss = { isDeleteDialogOpen = false }
        ) {
            Text(text = "The file will be deleted permanently.")
        }
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
            fullLineItem {
                OutlinedButton(onClick = { isDeleteDialogOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "ConfirmDialog: delete")
                }
            }

            header("Progress")
            fullLineItem { ProgressBar(progress = null, label = "ProgressBar, unknown progress") }
            fullLineItem { ProgressBar(progress = 0.42f, label = "ProgressBar, 42%", showPercent = true) }
            fullLineItem { AnimatedProgressBar() }
            fullLineItem {
                SampleBox(caption = "LoadingContent: full screen while opening") {
                    LoadingContent(text = "Opening…", modifier = Modifier.height(140.dp))
                }
            }
            fullLineItem {
                SampleBox(caption = "Inline spinner: PDF page, image, player (Material, not common-ui)") {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }

            header("Connection status")
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

/** Fills up again and again, to see how [ProgressBar] animates changes. */
@Composable
private fun AnimatedProgressBar() {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            progress = if (progress >= 1f) 0f else progress + 0.1f
        }
    }
    ProgressBar(progress = progress, label = "ProgressBar, changing", showPercent = true)
}

@Composable
private fun SampleBox(caption: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = caption, style = MaterialTheme.typography.bodyMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
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
