package com.engboost.monitoringdevices.feature.filemanager.impl.presentation

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.contract.CreateDocumentContract
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.ExportPickerUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.screen.FileManagerScreen
import java.io.File

@Composable
internal fun FileManagerFeatureScreen(
    onBackClick: () -> Unit,
    onFileOpen: (File) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FileManagerViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnFileOpen by rememberUpdatedState(onFileOpen)

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = viewModel::onImportFilesSelected
    )
    val importDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::onImportDirectorySelected
    )
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = CreateDocumentContract(),
        onResult = viewModel::onExportDocumentCreated
    )
    val exportToFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::onExportFolderPicked
    )

    // Files may be changed by other parts of the app while the screen is in background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenResumed() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onMessageShown()
    }

    LaunchedEffect(state.exportPicker) {
        val picker = state.exportPicker ?: return@LaunchedEffect
        viewModel.onExportPickerShown()
        when (picker) {
            is ExportPickerUi.CreateDocument -> exportFileLauncher.launchOrReport(
                picker.fileName,
                viewModel::onPickerUnavailable
            )

            ExportPickerUi.PickFolder -> exportToFolderLauncher.launchOrReport(null, viewModel::onPickerUnavailable)
        }
    }

    LaunchedEffect(state.fileToOpen) {
        val file = state.fileToOpen ?: return@LaunchedEffect
        viewModel.onFileOpenHandled()
        currentOnFileOpen(file)
    }

    // Selection mode and nested folders consume Back; at the root the host handles it.
    BackHandler(enabled = state.isSelectionMode || !state.isRoot, onBack = viewModel::onNavigateUp)

    FileManagerScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBackClick = {
            if (state.isRoot) onBackClick() else viewModel.onNavigateUp()
        },
        onBreadcrumbClick = viewModel::onBreadcrumbClick,
        onEntryClick = viewModel::onEntryClick,
        onEntryLongClick = viewModel::onEntryLongClick,
        onImportFilesClick = {
            importFilesLauncher.launchOrReport(arrayOf("*/*"), viewModel::onPickerUnavailable)
        },
        onImportDirectoryClick = {
            importDirectoryLauncher.launchOrReport(null, viewModel::onPickerUnavailable)
        },
        onCreateDirectoryClick = viewModel::onCreateDirectoryClick,
        onSelectionClose = viewModel::onSelectionClose,
        onSelectAllClick = viewModel::onSelectAllClick,
        onExportSelectedClick = viewModel::onExportSelectedClick,
        onDeleteSelectedClick = viewModel::onDeleteSelectedClick,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onCreateDirectoryConfirm = viewModel::onCreateDirectoryConfirm,
        onCreateDirectoryDismiss = viewModel::onCreateDirectoryDismiss,
        modifier = modifier
    )
}

private fun <I> ActivityResultLauncher<I>.launchOrReport(input: I, onUnavailable: () -> Unit) {
    try {
        launch(input)
    } catch (error: ActivityNotFoundException) {
        onUnavailable()
    }
}
