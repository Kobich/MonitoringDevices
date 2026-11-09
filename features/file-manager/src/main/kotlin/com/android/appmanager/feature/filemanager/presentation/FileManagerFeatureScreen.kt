package com.android.appmanager.feature.filemanager.presentation

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.android.appmanager.feature.filemanager.presentation.contract.CreateDocumentContract
import com.android.appmanager.feature.filemanager.presentation.model.PickerUi
import com.android.appmanager.feature.filemanager.presentation.screen.FileManagerScreen
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
        onResult = viewModel::onImportFilesPicked
    )
    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::onImportFolderPicked
    )
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = CreateDocumentContract(),
        onResult = viewModel::onExportDocumentCreated
    )
    val exportFolderLauncher = rememberLauncherForActivityResult(
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

    LaunchedEffect(state.picker) {
        val picker = state.picker ?: return@LaunchedEffect
        viewModel.onPickerShown()
        try {
            when (picker) {
                PickerUi.ImportFiles -> importFilesLauncher.launch(arrayOf("*/*"))
                PickerUi.ImportFolder -> importFolderLauncher.launch(null)
                is PickerUi.ExportFile -> exportFileLauncher.launch(picker.fileName)
                PickerUi.ExportFolder -> exportFolderLauncher.launch(null)
            }
        } catch (error: ActivityNotFoundException) {
            viewModel.onPickerUnavailable()
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
        actions = viewModel,
        snackbarHostState = snackbarHostState,
        onBackClick = { if (state.isRoot) onBackClick() else viewModel.onNavigateUp() },
        modifier = modifier
    )
}
