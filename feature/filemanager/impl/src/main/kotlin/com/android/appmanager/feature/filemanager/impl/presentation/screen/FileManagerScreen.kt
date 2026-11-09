package com.android.appmanager.feature.filemanager.impl.presentation.screen

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.appmanager.feature.filemanager.impl.R
import com.android.appmanager.feature.filemanager.impl.presentation.FileManagerActions
import com.android.appmanager.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.android.appmanager.feature.filemanager.impl.presentation.model.FileManagerUiState
import com.android.core.commonui.components.BackTopAppBar
import com.android.core.commonui.components.LoadingContent
import com.android.core.commonui.components.MessageContent
import com.android.core.commonui.components.rememberDelayedVisibility
import com.android.core.commonui.icons.AppIcons

@Composable
internal fun FileManagerScreen(
    state: FileManagerUiState,
    actions: FileManagerActions,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.pendingDeletion?.let { deletion ->
        DeleteConfirmationDialog(
            deletion = deletion,
            onConfirm = actions::onDeleteConfirm,
            onDismiss = actions::onDeleteDismiss
        )
    }

    state.createDirectoryDialog?.let { dialog ->
        CreateDirectoryDialog(
            error = dialog.error,
            isBusy = state.isBusy,
            onConfirm = actions::onCreateDirectoryConfirm,
            onDismiss = actions::onCreateDirectoryDismiss
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
                    actions = actions
                )
            } else {
                BrowseTopBar(
                    title = state.title,
                    isActionsEnabled = !state.isBusy,
                    onBackClick = onBackClick,
                    actions = actions
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
            val isProgressVisible = rememberDelayedVisibility(state.isBusy || state.isLoading)
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                if (isProgressVisible && state.entries.isNotEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            Breadcrumbs(
                breadcrumbs = state.breadcrumbs,
                isEnabled = !state.isSelectionMode,
                onClick = actions::onBreadcrumbClick
            )
            HorizontalDivider()

            when {
                state.isLoading && state.entries.isEmpty() -> {
                    if (isProgressVisible) LoadingContent()
                }

                state.entries.isEmpty() -> MessageContent(
                    title = stringResource(R.string.fm_empty_folder_title),
                    text = stringResource(R.string.fm_empty_folder_text)
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
                            onClick = { actions.onEntryClick(entry) },
                            onLongClick = { actions.onEntryLongClick(entry) }
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
    actions: FileManagerActions
) {
    BackTopAppBar(
        title = title,
        onNavigationClick = onBackClick,
        actions = { StorageActionsMenu(enabled = isActionsEnabled, actions = actions) }
    )
}

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    isActionsEnabled: Boolean,
    actions: FileManagerActions
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    BackTopAppBar(
        title = stringResource(R.string.fm_selected_count, selectedCount),
        onNavigationClick = actions::onSelectionClose,
        navigationIcon = AppIcons.Close,
        navigationContentDescription = stringResource(R.string.fm_close_selection),
        actions = {
            IconButton(onClick = actions::onDeleteSelectedClick, enabled = isActionsEnabled) {
                Icon(imageVector = AppIcons.Delete, contentDescription = stringResource(R.string.fm_delete_selected))
            }
            Box {
                IconButton(onClick = { isMenuExpanded = true }, enabled = isActionsEnabled) {
                    Icon(imageVector = AppIcons.More, contentDescription = stringResource(R.string.fm_selection_actions))
                }
                DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.fm_select_all)) },
                        onClick = {
                            isMenuExpanded = false
                            actions.onSelectAllClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.fm_export)) },
                        onClick = {
                            isMenuExpanded = false
                            actions.onExportSelectedClick()
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
    actions: FileManagerActions
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }, enabled = enabled) {
            Icon(imageVector = AppIcons.More, contentDescription = stringResource(R.string.fm_storage_actions))
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.fm_import_files)) },
                onClick = {
                    isExpanded = false
                    actions.onImportFilesClick()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.fm_import_folder)) },
                onClick = {
                    isExpanded = false
                    actions.onImportFolderClick()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.fm_new_folder)) },
                onClick = {
                    isExpanded = false
                    actions.onCreateDirectoryClick()
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
