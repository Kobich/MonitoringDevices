package com.engboost.monitoringdevices.feature.filemanager.impl.presentation

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.contract.CreateDocumentContract
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.contract.CreateDocumentRequest
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.screen.FileManagerScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FileManagerFeatureScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FileManagerViewModel = koinViewModel()
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

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
        onResult = viewModel::onExportDestinationSelected
    )
    val exportDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::onExportDestinationSelected
    )

    // Files may be changed by other parts of the app while the screen is in background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenResumed()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onMessageShown()
    }

    BackHandler(enabled = !state.isRoot, onBack = viewModel::onNavigateUp)

    FileManagerScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBackClick = {
            if (state.isRoot) onBackClick() else viewModel.onNavigateUp()
        },
        onBreadcrumbClick = viewModel::onBreadcrumbClick,
        onEntryClick = viewModel::onEntryClick,
        onImportFilesClick = {
            importFilesLauncher.launchOrReport(arrayOf("*/*"), viewModel::onPickerUnavailable)
        },
        onImportDirectoryClick = {
            importDirectoryLauncher.launchOrReport(null, viewModel::onPickerUnavailable)
        },
        onCreateDirectoryClick = viewModel::onCreateDirectoryClick,
        onExportClick = { entry ->
            viewModel.onExportRequested(entry)
            if (entry.isDirectory) {
                exportDirectoryLauncher.launchOrReport(null, viewModel::onPickerUnavailable)
            } else {
                exportFileLauncher.launchOrReport(
                    CreateDocumentRequest(fileName = entry.name, mimeType = entry.mimeType),
                    viewModel::onPickerUnavailable
                )
            }
        },
        onDeleteClick = viewModel::onDeleteClick,
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
