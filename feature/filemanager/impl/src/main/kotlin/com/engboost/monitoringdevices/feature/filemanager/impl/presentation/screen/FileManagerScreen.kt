package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileManagerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileManagerScreen(
    state: FileManagerUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onBreadcrumbClick: (BreadcrumbUi) -> Unit,
    onEntryClick: (FileEntryUi) -> Unit,
    onImportFilesClick: () -> Unit,
    onImportDirectoryClick: () -> Unit,
    onCreateDirectoryClick: () -> Unit,
    onExportClick: (FileEntryUi) -> Unit,
    onDeleteClick: (FileEntryUi) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onCreateDirectoryConfirm: (String) -> Unit,
    onCreateDirectoryDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.pendingDeletion?.let { entry ->
        DeleteConfirmationDialog(
            entry = entry,
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss
        )
    }

    state.createDirectoryDialog?.let { dialog ->
        CreateDirectoryDialog(
            error = dialog.error,
            isBusy = state.isBusy,
            onConfirm = onCreateDirectoryConfirm,
            onDismiss = onCreateDirectoryDismiss
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = state.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    StorageActionsMenu(
                        enabled = !state.isBusy,
                        onImportFilesClick = onImportFilesClick,
                        onImportDirectoryClick = onImportDirectoryClick,
                        onCreateDirectoryClick = onCreateDirectoryClick
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                if (state.isBusy || (state.isLoading && state.entries.isNotEmpty())) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            Breadcrumbs(breadcrumbs = state.breadcrumbs, onClick = onBreadcrumbClick)
            HorizontalDivider()

            when {
                state.isLoading && state.entries.isEmpty() -> CenteredContent {
                    CircularProgressIndicator()
                }

                state.entries.isEmpty() -> CenteredContent {
                    EmptyFolderMessage()
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(items = state.entries, key = { entry -> entry.path }) { entry ->
                        FileEntryRow(
                            entry = entry,
                            isActionsEnabled = !state.isBusy,
                            onOpenClick = { onEntryClick(entry) },
                            onExportClick = { onExportClick(entry) },
                            onDeleteClick = { onDeleteClick(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageActionsMenu(
    enabled: Boolean,
    onImportFilesClick: () -> Unit,
    onImportDirectoryClick: () -> Unit,
    onCreateDirectoryClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }, enabled = enabled) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Storage actions")
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(text = "Import files") },
                onClick = {
                    isExpanded = false
                    onImportFilesClick()
                }
            )
            DropdownMenuItem(
                text = { Text(text = "Import folder") },
                onClick = {
                    isExpanded = false
                    onImportDirectoryClick()
                }
            )
            DropdownMenuItem(
                text = { Text(text = "New folder") },
                onClick = {
                    isExpanded = false
                    onCreateDirectoryClick()
                }
            )
        }
    }
}

@Composable
private fun Breadcrumbs(
    breadcrumbs: List<BreadcrumbUi>,
    onClick: (BreadcrumbUi) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(breadcrumbs.size) {
        if (breadcrumbs.isNotEmpty()) {
            listState.animateScrollToItem(breadcrumbs.lastIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(items = breadcrumbs, key = { _, breadcrumb -> breadcrumb.path }) { index, breadcrumb ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    Text(text = "›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onClick(breadcrumb) }, enabled = index != breadcrumbs.lastIndex) {
                    Text(text = breadcrumb.title, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun EmptyFolderMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Folder is empty", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Use the menu in the top bar to import files or create a folder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
