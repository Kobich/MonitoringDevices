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
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.ExportDestination
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.mapper.FileManagerUiMapper
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.CreateDirectoryDialogUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.ExportPickerUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileManagerUiState
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.SelectionUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gestures:
 * - browse mode: tap opens a folder or a file, long press enters selection mode
 *   with the pressed item selected;
 * - selection mode: tap or long press toggles an item, deselecting the last one
 *   (or Close / Back) returns to browse mode.
 */
@HiltViewModel
internal class FileManagerViewModel @Inject constructor(
    private val interactor: FileManagerInteractor,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val uiMapper = FileManagerUiMapper()

    // Kept in SavedStateHandle to survive process death, e.g. while a system picker is open.
    private var currentPath: StoragePath
        get() = StoragePath(savedStateHandle.get<String>(KEY_CURRENT_PATH).orEmpty())
        set(value) {
            savedStateHandle[KEY_CURRENT_PATH] = value.value
        }
    private var pendingExportPaths: List<StoragePath>
        get() = savedStateHandle.get<ArrayList<String>>(KEY_PENDING_EXPORT).orEmpty().map(::StoragePath)
        set(value) {
            savedStateHandle[KEY_PENDING_EXPORT] = ArrayList(value.map { path -> path.value })
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
            selection = null,
            pendingDeletion = null,
            createDirectoryDialog = null,
            message = null,
            exportPicker = null,
            fileToOpen = null
        )
    )
        private set

    private var deletionCandidates: List<FileEntryUi> = emptyList()

    init {
        loadDirectory(currentPath)
    }

    fun onScreenResumed() {
        loadDirectory(currentPath)
    }

    // region Navigation and gestures

    fun onEntryClick(entry: FileEntryUi) {
        when {
            uiState.isSelectionMode -> toggleSelection(entry)
            entry.isDirectory -> loadDirectory(StoragePath(entry.path))
            else -> openFile(entry)
        }
    }

    fun onEntryLongClick(entry: FileEntryUi) {
        if (uiState.isSelectionMode) {
            toggleSelection(entry)
        } else {
            uiState = uiState.copy(selection = SelectionUi(setOf(entry.path)))
        }
    }

    fun onBreadcrumbClick(breadcrumb: BreadcrumbUi) {
        loadDirectory(StoragePath(breadcrumb.path))
    }

    /** System Back / top bar arrow: leaves selection mode first, then goes up. */
    fun onNavigateUp() {
        if (uiState.isSelectionMode) {
            onSelectionClose()
        } else {
            currentPath.parent()?.let(::loadDirectory)
        }
    }

    fun onFileOpenHandled() {
        uiState = uiState.copy(fileToOpen = null)
    }

    // endregion

    // region Selection

    fun onSelectionClose() {
        uiState = uiState.copy(selection = null)
    }

    fun onSelectAllClick() {
        val allPaths = uiState.entries.map { entry -> entry.path }.toSet()
        if (allPaths.isNotEmpty()) {
            uiState = uiState.copy(selection = SelectionUi(allPaths))
        }
    }

    fun onDeleteSelectedClick() {
        val entries = selectedEntries()
        if (entries.isEmpty()) return

        deletionCandidates = entries
        uiState = uiState.copy(pendingDeletion = uiMapper.deletion(entries))
    }

    fun onExportSelectedClick() {
        val entries = selectedEntries()
        if (entries.isEmpty()) return

        val paths = entries.map { entry -> StoragePath(entry.path) }
        val singleFile = entries.singleOrNull()?.takeIf { entry -> !entry.isDirectory }
        val destination = interactor.exportDestination(paths, isSingleFile = singleFile != null)
        val picker = if (destination == ExportDestination.DOCUMENT && singleFile != null) {
            ExportPickerUi.CreateDocument(fileName = singleFile.name, mimeType = singleFile.mimeType)
        } else {
            ExportPickerUi.PickFolder
        }

        pendingExportPaths = paths
        uiState = uiState.copy(exportPicker = picker)
    }

    private fun toggleSelection(entry: FileEntryUi) {
        val selected = uiState.selection?.selectedPaths.orEmpty()
        val updated = if (entry.path in selected) selected - entry.path else selected + entry.path
        uiState = uiState.copy(selection = updated.takeIf { it.isNotEmpty() }?.let(::SelectionUi))
    }

    private fun selectedEntries(): List<FileEntryUi> {
        val selection = uiState.selection ?: return emptyList()
        return uiState.entries.filter { entry -> entry in selection }
    }

    // endregion

    // region Storage operations

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

    fun onExportPickerShown() {
        uiState = uiState.copy(exportPicker = null)
    }

    fun onExportDestinationSelected(uri: Uri?) {
        val paths = pendingExportPaths
        if (paths.isEmpty()) return
        pendingExportPaths = emptyList()
        uri ?: return

        uiState = uiState.copy(selection = null)
        runOperation(failurePrefix = "Export failed") {
            uiMapper.exportedText(interactor.export(paths, uri))
        }
    }

    fun onPickerUnavailable() {
        pendingExportPaths = emptyList()
        uiState = uiState.copy(message = "No app available to pick a document")
    }

    fun onDeleteDismiss() {
        deletionCandidates = emptyList()
        uiState = uiState.copy(pendingDeletion = null)
    }

    fun onDeleteConfirm() {
        val entries = deletionCandidates
        if (entries.isEmpty()) return
        deletionCandidates = emptyList()
        uiState = uiState.copy(pendingDeletion = null, selection = null)

        runOperation(failurePrefix = "Delete failed") {
            interactor.delete(entries.map { entry -> StoragePath(entry.path) })
            uiMapper.deletedText(entries)
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
                }

                is CreateDirectoryResult.Invalid -> {
                    uiState = uiState.copy(
                        createDirectoryDialog = CreateDirectoryDialogUi(uiMapper.errorText(result.error))
                    )
                }
            }
            null
        }
    }

    fun onMessageShown() {
        uiState = uiState.copy(message = null)
    }

    // endregion

    private fun openFile(entry: FileEntryUi) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(fileToOpen = interactor.fileForOpening(StoragePath(entry.path)))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                uiState = uiState.copy(message = "Can't open file: ${error.toDisplayMessage()}")
            }
        }
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
        val isSameFolder = content.path == currentPath
        currentPath = content.path

        val entries = content.entries.map(uiMapper::toUi)
        // A refresh keeps the selection of items that still exist; another folder resets it.
        val selection = uiState.selection
            ?.takeIf { isSameFolder }
            ?.selectedPaths
            ?.intersect(entries.map { entry -> entry.path }.toSet())
            ?.takeIf { paths -> paths.isNotEmpty() }
            ?.let(::SelectionUi)

        uiState = uiState.copy(
            title = uiMapper.title(content.path),
            breadcrumbs = uiMapper.breadcrumbs(content.path),
            isRoot = content.path.isRoot,
            entries = entries,
            isLoading = false,
            selection = selection
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
