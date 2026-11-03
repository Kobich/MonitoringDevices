package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.screen

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.core.commonui.components.BackTopAppBar
import com.engboost.monitoringdevices.core.commonui.components.LoadingContent
import com.engboost.monitoringdevices.core.commonui.components.MessageContent
import com.engboost.monitoringdevices.core.commonui.components.rememberDelayedVisibility
import com.engboost.monitoringdevices.core.commonui.icons.AppIcons
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileManagerUiState

@Composable
internal fun FileManagerScreen(
    state: FileManagerUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onBreadcrumbClick: (BreadcrumbUi) -> Unit,
    onEntryClick: (FileEntryUi) -> Unit,
    onEntryLongClick: (FileEntryUi) -> Unit,
    onImportFilesClick: () -> Unit,
    onImportDirectoryClick: () -> Unit,
    onCreateDirectoryClick: () -> Unit,
    onSelectionClose: () -> Unit,
    onSelectAllClick: () -> Unit,
    onExportSelectedClick: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onCreateDirectoryConfirm: (String) -> Unit,
    onCreateDirectoryDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.pendingDeletion?.let { deletion ->
        DeleteConfirmationDialog(
            deletion = deletion,
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
            val selection = state.selection
            if (selection != null) {
                SelectionTopBar(
                    selectedCount = selection.count,
                    isActionsEnabled = !state.isBusy,
                    onCloseClick = onSelectionClose,
                    onSelectAllClick = onSelectAllClick,
                    onExportClick = onExportSelectedClick,
                    onDeleteClick = onDeleteSelectedClick
                )
            } else {
                BrowseTopBar(
                    title = state.title,
                    isActionsEnabled = !state.isBusy,
                    onBackClick = onBackClick,
                    onImportFilesClick = onImportFilesClick,
                    onImportDirectoryClick = onImportDirectoryClick,
                    onCreateDirectoryClick = onCreateDirectoryClick
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Folder reads are usually instant: show progress only when they take noticeable time.
            val isProgressVisible = rememberDelayedVisibility(state.isBusy || state.isLoading)
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                if (isProgressVisible && state.entries.isNotEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            Breadcrumbs(
                breadcrumbs = state.breadcrumbs,
                isEnabled = !state.isSelectionMode,
                onClick = onBreadcrumbClick
            )
            HorizontalDivider()

            when {
                state.isLoading && state.entries.isEmpty() -> {
                    if (isProgressVisible) LoadingContent()
                }

                state.entries.isEmpty() -> MessageContent(
                    title = "Folder is empty",
                    text = "Use the menu in the top bar to import files or create a folder. " +
                        "Long press an item to select it."
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(items = state.entries, key = { entry -> entry.path }) { entry ->
                        FileEntryRow(
                            entry = entry,
                            isSelectionMode = state.isSelectionMode,
                            isSelected = state.selection?.contains(entry) == true,
                            onClick = { onEntryClick(entry) },
                            onLongClick = { onEntryLongClick(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseTopBar(
    title: String,
    isActionsEnabled: Boolean,
    onBackClick: () -> Unit,
    onImportFilesClick: () -> Unit,
    onImportDirectoryClick: () -> Unit,
    onCreateDirectoryClick: () -> Unit
) {
    BackTopAppBar(
        title = title,
        onNavigationClick = onBackClick,
        actions = {
            StorageActionsMenu(
                enabled = isActionsEnabled,
                onImportFilesClick = onImportFilesClick,
                onImportDirectoryClick = onImportDirectoryClick,
                onCreateDirectoryClick = onCreateDirectoryClick
            )
        }
    )
}

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    isActionsEnabled: Boolean,
    onCloseClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    BackTopAppBar(
        title = "$selectedCount selected",
        onNavigationClick = onCloseClick,
        navigationIcon = AppIcons.Close,
        navigationContentDescription = "Close selection",
        actions = {
            IconButton(onClick = onDeleteClick, enabled = isActionsEnabled) {
                Icon(imageVector = AppIcons.Delete, contentDescription = "Delete selected")
            }
            Box {
                IconButton(onClick = { isMenuExpanded = true }, enabled = isActionsEnabled) {
                    Icon(imageVector = AppIcons.More, contentDescription = "Selection actions")
                }
                DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(text = "Select all") },
                        onClick = {
                            isMenuExpanded = false
                            onSelectAllClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Export") },
                        onClick = {
                            isMenuExpanded = false
                            onExportClick()
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    )
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
            Icon(imageVector = AppIcons.More, contentDescription = "Storage actions")
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
    isEnabled: Boolean,
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
                TextButton(onClick = { onClick(breadcrumb) }, enabled = isEnabled && index != breadcrumbs.lastIndex) {
                    Text(text = breadcrumb.title, maxLines = 1)
                }
            }
        }
    }
}
