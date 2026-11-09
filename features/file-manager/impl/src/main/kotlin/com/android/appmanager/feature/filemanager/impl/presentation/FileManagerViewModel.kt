package com.android.appmanager.feature.filemanager.impl.presentation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.appmanager.feature.filemanager.impl.R
import com.android.appmanager.feature.filemanager.impl.domain.interactor.FileManagerInteractor
import com.android.appmanager.feature.filemanager.impl.domain.model.DirectoryContent
import com.android.appmanager.feature.filemanager.impl.domain.model.StoragePath
import com.android.appmanager.feature.filemanager.impl.domain.model.TransferSummary
import com.android.appmanager.feature.filemanager.impl.presentation.mapper.FileManagerUiMapper
import com.android.appmanager.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.android.appmanager.feature.filemanager.impl.presentation.model.CreateDirectoryDialogUi
import com.android.appmanager.feature.filemanager.impl.presentation.model.FileEntryUi
import com.android.appmanager.feature.filemanager.impl.presentation.model.FileManagerUiState
import com.android.appmanager.feature.filemanager.impl.presentation.model.PickerUi
import com.android.appmanager.feature.filemanager.impl.presentation.model.SelectionUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class FileManagerViewModel @Inject constructor(
    private val interactor: FileManagerInteractor,
    private val uiMapper: FileManagerUiMapper,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), FileManagerActions {

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
            picker = null,
            fileToOpen = null
        )
    )
        private set

    init {
        loadDirectory(currentPath)
    }

    fun onScreenResumed() {
        loadDirectory(currentPath)
    }

    override fun onEntryClick(entry: FileEntryUi) {
        when {
            uiState.isSelectionMode -> toggleSelection(entry)
            entry.isDirectory -> loadDirectory(StoragePath(entry.path))
            else -> openFile(entry)
        }
    }

    override fun onEntryLongClick(entry: FileEntryUi) {
        if (uiState.isSelectionMode) {
            toggleSelection(entry)
        } else {
            uiState = uiState.copy(selection = SelectionUi(setOf(entry.path)))
        }
    }

    override fun onBreadcrumbClick(breadcrumb: BreadcrumbUi) {
        loadDirectory(StoragePath(breadcrumb.path))
    }

    /** Leaves selection mode first, then goes up. */
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

    override fun onSelectionClose() {
        uiState = uiState.copy(selection = null)
    }

    override fun onSelectAllClick() {
        val allPaths = uiState.entries.map { entry -> entry.path }.toSet()
        if (allPaths.isNotEmpty()) {
            uiState = uiState.copy(selection = SelectionUi(allPaths))
        }
    }

    override fun onDeleteSelectedClick() {
        val entries = selectedEntries()
        if (entries.isEmpty()) return

        uiState = uiState.copy(pendingDeletion = uiMapper.deletion(entries))
    }

    override fun onExportSelectedClick() {
        val entries = selectedEntries()
        if (entries.isEmpty()) return

        val singleFile = entries.singleOrNull()?.takeIf { entry -> !entry.isDirectory }
        val picker = if (singleFile != null) PickerUi.ExportFile(singleFile.name) else PickerUi.ExportFolder

        pendingExportPaths = entries.map { entry -> StoragePath(entry.path) }
        uiState = uiState.copy(picker = picker)
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

    override fun onImportFilesClick() {
        uiState = uiState.copy(picker = PickerUi.ImportFiles)
    }

    override fun onImportFolderClick() {
        uiState = uiState.copy(picker = PickerUi.ImportFolder)
    }

    fun onPickerShown() {
        uiState = uiState.copy(picker = null)
    }

    fun onPickerUnavailable() {
        pendingExportPaths = emptyList()
        uiState = uiState.copy(message = uiMapper.text(R.string.fm_no_picker))
    }

    fun onImportFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val destination = currentPath
        runOperation(R.string.fm_import_failed) {
            uiMapper.importedText(interactor.importFiles(uris, destination))
        }
    }

    fun onImportFolderPicked(uri: Uri?) {
        uri ?: return
        val destination = currentPath
        runOperation(R.string.fm_import_failed) {
            uiMapper.importedText(interactor.importDirectory(uri, destination))
        }
    }

    fun onExportDocumentCreated(uri: Uri?) {
        val path = takePendingExport()?.singleOrNull() ?: return
        uri ?: return
        export { interactor.exportFile(path, uri) }
    }

    fun onExportFolderPicked(uri: Uri?) {
        val paths = takePendingExport() ?: return
        uri ?: return
        export { interactor.exportToFolder(paths, uri) }
    }

    private fun takePendingExport(): List<StoragePath>? {
        val paths = pendingExportPaths.takeIf { it.isNotEmpty() }
        pendingExportPaths = emptyList()
        return paths
    }

    private fun export(block: suspend () -> TransferSummary) {
        uiState = uiState.copy(selection = null)
        runOperation(R.string.fm_export_failed) { uiMapper.exportedText(block()) }
    }

    override fun onDeleteDismiss() {
        uiState = uiState.copy(pendingDeletion = null)
    }

    override fun onDeleteConfirm() {
        val entries = uiState.pendingDeletion?.entries ?: return
        uiState = uiState.copy(pendingDeletion = null, selection = null)

        runOperation(R.string.fm_delete_failed) {
            interactor.delete(entries.map { entry -> StoragePath(entry.path) })
            uiMapper.deletedText(entries)
        }
    }

    override fun onCreateDirectoryClick() {
        uiState = uiState.copy(createDirectoryDialog = CreateDirectoryDialogUi(error = null))
    }

    override fun onCreateDirectoryDismiss() {
        uiState = uiState.copy(createDirectoryDialog = null)
    }

    override fun onCreateDirectoryConfirm(name: String) {
        val parent = currentPath
        runOperation(R.string.fm_create_folder_failed) {
            val error = interactor.createDirectory(parent, name)
            uiState = uiState.copy(
                createDirectoryDialog = error?.let { CreateDirectoryDialogUi(uiMapper.errorText(it)) }
            )
            null
        }
    }

    fun onMessageShown() {
        uiState = uiState.copy(message = null)
    }

    private fun openFile(entry: FileEntryUi) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(fileToOpen = interactor.fileOf(StoragePath(entry.path)))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                uiState = uiState.copy(message = uiMapper.failureText(R.string.fm_open_file_failed, error))
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
                    message = uiMapper.failureText(R.string.fm_open_folder_failed, error)
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

    /** One operation at a time; [block] returns a message to show, the folder is reloaded afterwards. */
    private fun runOperation(@StringRes failure: Int, block: suspend () -> String?) {
        if (operationJob?.isActive == true) {
            uiState = uiState.copy(message = uiMapper.text(R.string.fm_operation_running))
            return
        }

        uiState = uiState.copy(isBusy = true)
        operationJob = viewModelScope.launch {
            val message = try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                uiMapper.failureText(failure, error)
            }

            uiState = uiState.copy(isBusy = false, message = message ?: uiState.message)
            loadDirectory(currentPath)
        }
    }

    private companion object {
        const val KEY_CURRENT_PATH = "file_manager.current_path"
        const val KEY_PENDING_EXPORT = "file_manager.pending_export"
    }
}
