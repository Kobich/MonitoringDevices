package com.engboost.monitoringdevices.feature.filemanager.impl.presentation

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.interactor.FileManagerInteractor
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.CreateDirectoryResult
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.DirectoryContent
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.mapper.FileManagerUiMapper
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.CreateDirectoryDialogUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileManagerUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class FileManagerViewModel(
    private val interactor: FileManagerInteractor,
    private val savedStateHandle: SavedStateHandle,
    private val uiMapper: FileManagerUiMapper = FileManagerUiMapper()
) : ViewModel() {
    // Kept in SavedStateHandle to survive process death, e.g. while a system picker is open.
    private var currentPath: StoragePath
        get() = StoragePath(savedStateHandle.get<String>(KEY_CURRENT_PATH).orEmpty())
        set(value) {
            savedStateHandle[KEY_CURRENT_PATH] = value.value
        }
    private var pendingExportPath: StoragePath?
        get() = savedStateHandle.get<String>(KEY_PENDING_EXPORT)?.let(::StoragePath)
        set(value) {
            savedStateHandle[KEY_PENDING_EXPORT] = value?.value
        }

    private var loadJob: Job? = null
    private var operationJob: Job? = null

    var uiState by mutableStateOf(
        FileManagerUiState(
            title = uiMapper.title(currentPath),
            breadcrumbs = uiMapper.breadcrumbs(currentPath),
            isRoot = currentPath.isRoot,
            entries = emptyList(),
            isLoading = true,
            isBusy = false,
            message = null,
            pendingDeletion = null,
            createDirectoryDialog = null
        )
    )
        private set

    init {
        loadDirectory(currentPath)
    }

    fun onScreenResumed() {
        loadDirectory(currentPath)
    }

    fun onEntryClick(entry: FileEntryUi) {
        if (entry.isDirectory) {
            loadDirectory(StoragePath(entry.path))
        }
    }

    fun onBreadcrumbClick(breadcrumb: BreadcrumbUi) {
        loadDirectory(StoragePath(breadcrumb.path))
    }

    fun onNavigateUp() {
        currentPath.parent()?.let(::loadDirectory)
    }

    fun onImportFilesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val destination = currentPath
        runOperation(failurePrefix = "Import failed") {
            uiMapper.importedText(interactor.importFiles(uris, destination))
        }
    }

    fun onImportDirectorySelected(uri: Uri?) {
        uri ?: return
        val destination = currentPath
        runOperation(failurePrefix = "Import failed") {
            uiMapper.importedText(interactor.importDirectory(uri, destination))
        }
    }

    /** Must be called right before the export destination picker is launched. */
    fun onExportRequested(entry: FileEntryUi) {
        pendingExportPath = StoragePath(entry.path)
    }

    fun onExportDestinationSelected(uri: Uri?) {
        val path = pendingExportPath ?: return
        pendingExportPath = null
        uri ?: return

        runOperation(failurePrefix = "Export failed") {
            uiMapper.exportedText(path.name, interactor.export(path, uri))
        }
    }

    fun onPickerUnavailable() {
        pendingExportPath = null
        uiState = uiState.copy(message = "No app available to pick a document")
    }

    fun onDeleteClick(entry: FileEntryUi) {
        uiState = uiState.copy(pendingDeletion = entry)
    }

    fun onDeleteDismiss() {
        uiState = uiState.copy(pendingDeletion = null)
    }

    fun onDeleteConfirm() {
        val entry = uiState.pendingDeletion ?: return
        uiState = uiState.copy(pendingDeletion = null)

        runOperation(failurePrefix = "Delete failed") {
            interactor.delete(StoragePath(entry.path))
            "Deleted \"${entry.name}\""
        }
    }

    fun onCreateDirectoryClick() {
        uiState = uiState.copy(createDirectoryDialog = CreateDirectoryDialogUi(error = null))
    }

    fun onCreateDirectoryDismiss() {
        uiState = uiState.copy(createDirectoryDialog = null)
    }

    fun onCreateDirectoryConfirm(name: String) {
        val parent = currentPath
        runOperation(failurePrefix = "Can't create folder") {
            when (val result = interactor.createDirectory(parent, name)) {
                is CreateDirectoryResult.Created -> {
                    uiState = uiState.copy(createDirectoryDialog = null)
                    null
                }

                is CreateDirectoryResult.Invalid -> {
                    uiState = uiState.copy(
                        createDirectoryDialog = CreateDirectoryDialogUi(uiMapper.errorText(result.error))
                    )
                    null
                }
            }
        }
    }

    fun onMessageShown() {
        uiState = uiState.copy(message = null)
    }

    private fun loadDirectory(path: StoragePath) {
        loadJob?.cancel()
        // The previous folder stays on screen until the new one is read, so a failed
        // navigation leaves a consistent state.
        uiState = uiState.copy(isLoading = true)

        loadJob = viewModelScope.launch {
            try {
                showDirectory(interactor.openDirectory(path))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    message = "Can't open folder: ${error.toDisplayMessage()}"
                )
            }
        }
    }

    private fun showDirectory(content: DirectoryContent) {
        currentPath = content.path
        uiState = uiState.copy(
            title = uiMapper.title(content.path),
            breadcrumbs = uiMapper.breadcrumbs(content.path),
            isRoot = content.path.isRoot,
            entries = content.entries.map(uiMapper::toUi),
            isLoading = false
        )
    }

    /**
     * Runs a modifying operation, one at a time. [block] returns a message to show
     * (or `null`), the current folder is reloaded afterwards.
     */
    private fun runOperation(failurePrefix: String, block: suspend () -> String?) {
        if (operationJob?.isActive == true) {
            uiState = uiState.copy(message = "Wait for the current operation to finish")
            return
        }

        uiState = uiState.copy(isBusy = true)
        operationJob = viewModelScope.launch {
            val message = try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                "$failurePrefix: ${error.toDisplayMessage()}"
            }

            uiState = uiState.copy(isBusy = false, message = message ?: uiState.message)
            loadDirectory(currentPath)
        }
    }

    private fun Throwable.toDisplayMessage(): String {
        return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
    }

    private companion object {
        const val KEY_CURRENT_PATH = "file_manager.current_path"
        const val KEY_PENDING_EXPORT = "file_manager.pending_export"
    }
}
