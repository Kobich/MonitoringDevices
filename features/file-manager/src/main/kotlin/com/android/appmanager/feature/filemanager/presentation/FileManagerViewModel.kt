package com.android.appmanager.feature.filemanager.presentation

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.appmanager.feature.filemanager.FilePasswords
import com.android.appmanager.feature.filemanager.R
import com.android.appmanager.feature.filemanager.data.repository.FileStorageRepository
import com.android.appmanager.feature.filemanager.domain.interactor.FileManagerInteractor
import com.android.appmanager.feature.filemanager.domain.model.DirectoryContent
import com.android.appmanager.feature.filemanager.domain.model.StoragePath
import com.android.appmanager.feature.filemanager.domain.model.TransferSummary
import com.android.appmanager.feature.filemanager.presentation.mapper.FileManagerUiMapper
import com.android.appmanager.feature.filemanager.presentation.model.BreadcrumbUi
import com.android.appmanager.feature.filemanager.presentation.model.CreateDirectoryDialogUi
import com.android.appmanager.feature.filemanager.presentation.model.FileEntryUi
import com.android.appmanager.feature.filemanager.presentation.model.FileManagerUiState
import com.android.appmanager.feature.filemanager.presentation.model.PasswordDialogUi
import com.android.appmanager.feature.filemanager.presentation.model.PickerUi
import com.android.appmanager.feature.filemanager.presentation.model.SelectionUi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "FileManager"

/**
 * Shows the files under [rootDirectory], the folder of a `FileStorage`. Files in [passwords] are opened, exported and
 * deleted only after their password; an entered password holds until the screen is closed.
 */
@HiltViewModel(assistedFactory = FileManagerViewModel.Factory::class)
internal class FileManagerViewModel @AssistedInject constructor(
    @Assisted rootDirectory: File,
    @Assisted private var passwords: FilePasswords?,
    repositoryFactory: FileStorageRepository.Factory,
    private val uiMapper: FileManagerUiMapper,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), FileManagerActions {

    private val interactor = FileManagerInteractor(repositoryFactory.create(rootDirectory))

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

    // In memory only: a password shouldn't outlive the screen, not even through process death.
    private val unlockedEntries = mutableSetOf<FilePasswords.Entry>()
    private var pendingUnlock: PendingUnlock? = null

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
            passwordDialog = null,
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

    /** New passwords forget the ones entered for the old. */
    fun onPasswordsChanged(passwords: FilePasswords?) {
        if (passwords == this.passwords) return
        Log.d(TAG, "passwords: ${passwords?.entries?.size ?: 0} entry(s)")
        this.passwords = passwords
        unlockedEntries.clear()
        pendingUnlock = null
        uiState = uiState.copy(entries = uiState.entries.withLocks(), passwordDialog = null)
    }

    override fun onEntryClick(entry: FileEntryUi) {
        when {
            uiState.isSelectionMode -> toggleSelection(entry)
            entry.isDirectory -> loadDirectory(StoragePath(entry.path))
            else -> {
                logClick("open_file", "list_item", "File")
                withUnlocked(listOf(StoragePath(entry.path))) { openFile(entry) }
            }
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

        withUnlocked(entries.map { entry -> StoragePath(entry.path) }) {
            uiState = uiState.copy(pendingDeletion = uiMapper.deletion(entries))
        }
    }

    override fun onExportSelectedClick() {
        logClick("export", "menu_item", "Export")
        val entries = selectedEntries()
        if (entries.isEmpty()) return

        withUnlocked(entries.map { entry -> StoragePath(entry.path) }) { requestExport(entries) }
    }

    private fun requestExport(entries: List<FileEntryUi>) {
        val singleFile = entries.singleOrNull()?.takeIf { entry -> !entry.isDirectory }
        val picker = if (singleFile != null) PickerUi.ExportFile(singleFile.name) else PickerUi.ExportFolder

        pendingExportPaths = entries.map { entry -> StoragePath(entry.path) }
        Log.d(TAG, "export: ${entries.size} item(s), picker=$picker")
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
        logClick("import_files", "menu_item", "Import files")
        uiState = uiState.copy(picker = PickerUi.ImportFiles)
    }

    override fun onImportFolderClick() {
        logClick("import_folder", "menu_item", "Import folder")
        uiState = uiState.copy(picker = PickerUi.ImportFolder)
    }

    fun onPickerShown() {
        uiState = uiState.copy(picker = null)
    }

    fun onPickerUnavailable() {
        Log.w(TAG, "picker: no app to handle it")
        pendingExportPaths = emptyList()
        uiState = uiState.copy(message = uiMapper.text(R.string.fm_no_picker))
    }

    fun onImportFilesPicked(uris: List<Uri>) {
        Log.d(TAG, "importFiles: picked ${uris.size} file(s) into $currentPath")
        if (uris.isEmpty()) return
        val destination = currentPath
        runOperation(R.string.fm_import_failed) {
            uiMapper.importedText(interactor.importFiles(uris, destination))
        }
    }

    fun onImportFolderPicked(uri: Uri?) {
        Log.d(TAG, "importFolder: picked=${uri != null} into $currentPath")
        uri ?: return
        val destination = currentPath
        runOperation(R.string.fm_import_failed) {
            uiMapper.importedText(interactor.importDirectory(uri, destination))
        }
    }

    fun onExportDocumentCreated(uri: Uri?) {
        val path = takePendingExport()?.singleOrNull() ?: return
        Log.d(TAG, "exportFile: $path, document created=${uri != null}")
        uri ?: return
        export { interactor.exportFile(path, uri) }
    }

    fun onExportFolderPicked(uri: Uri?) {
        val paths = takePendingExport() ?: return
        Log.d(TAG, "exportToFolder: ${paths.size} item(s), folder picked=${uri != null}")
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
        logClick("delete_confirm", "dialog_button", "Delete")
        val entries = uiState.pendingDeletion?.entries ?: return
        Log.d(TAG, "delete: ${entries.map { it.path }}")
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
        logClick("create_folder_confirm", "dialog_button", "Create")
        val parent = currentPath
        runOperation(R.string.fm_create_folder_failed) {
            val error = interactor.createDirectory(parent, name)
            Log.d(TAG, "createDirectory: \"$name\" in $parent, error=$error")
            uiState = uiState.copy(
                createDirectoryDialog = error?.let { CreateDirectoryDialogUi(uiMapper.errorText(it)) }
            )
            null
        }
    }

    override fun onPasswordConfirm(password: String) {
        logClick("password_confirm", "dialog_button", "Unlock")
        val pending = pendingUnlock ?: return
        val dialog = uiState.passwordDialog ?: return
        if (dialog.isChecking || password.isEmpty()) return

        uiState = uiState.copy(passwordDialog = dialog.copy(isChecking = true))
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val chars = password.toCharArray()
                try {
                    runCatching { pending.entry.password.matches(chars) }
                } finally {
                    chars.fill('\u0000')
                }
            }
            if (pendingUnlock !== pending) return@launch

            val matches = result.getOrElse { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "password: can't check", error)
                uiState = uiState.copy(passwordDialog = dialog.copy(error = uiMapper.text(R.string.fm_password_check_failed)))
                return@launch
            }
            Log.d(TAG, "password: matches=$matches")
            if (!matches) {
                uiState = uiState.copy(passwordDialog = dialog.copy(error = uiMapper.text(R.string.fm_password_wrong)))
                return@launch
            }

            unlockedEntries += pending.entry
            pendingUnlock = null
            uiState = uiState.copy(passwordDialog = null, entries = uiState.entries.withLocks())
            // Other files of the action may have passwords of their own: each is asked in turn.
            withUnlocked(pending.paths, pending.action)
        }
    }

    override fun onPasswordDismiss() {
        pendingUnlock = null
        uiState = uiState.copy(passwordDialog = null)
    }

    /**
     * Runs [action] on [paths] (files, or folders with everything in them) once every password protecting them is
     * entered, asking for the first missing one otherwise.
     */
    private fun withUnlocked(paths: List<StoragePath>, action: () -> Unit) {
        val locked = paths.firstNotNullOfOrNull { path ->
            passwords?.entriesCovering(path)?.firstOrNull { entry -> entry !in unlockedEntries }
                ?.let { entry -> path to entry }
        }
        if (locked == null) {
            action()
            return
        }

        val (path, entry) = locked
        // The protected file itself, or the first one inside the selected folder.
        val fileName = entry.storagePaths.first { file -> file.isInside(path) }.name
        pendingUnlock = PendingUnlock(paths, entry, action)
        uiState = uiState.copy(passwordDialog = PasswordDialogUi(fileName = fileName, error = null, isChecking = false))
    }

    private fun List<FileEntryUi>.withLocks(): List<FileEntryUi> = map { entry ->
        val lock = if (entry.isDirectory) null else passwords?.entryOf(StoragePath(entry.path))
        entry.copy(isLocked = lock != null && lock !in unlockedEntries)
    }

    fun onMessageShown() {
        uiState = uiState.copy(message = null)
    }

    private fun openFile(entry: FileEntryUi) {
        viewModelScope.launch {
            try {
                val file = interactor.fileOf(StoragePath(entry.path))
                Log.d(TAG, "openFile: ${file.path}")
                uiState = uiState.copy(fileToOpen = file)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "openFile: ${entry.path} failed", error)
                uiState = uiState.copy(message = uiMapper.failureText(R.string.fm_open_file_failed, error))
            }
        }
    }

    private fun loadDirectory(path: StoragePath) {
        loadJob?.cancel()
        // The previous folder stays on screen until the new one is read, so a failed
        // navigation leaves a consistent state.
        uiState = uiState.copy(isLoading = true)
        Log.d(TAG, "loadDirectory: $path")

        loadJob = viewModelScope.launch {
            try {
                showDirectory(interactor.openDirectory(path))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "loadDirectory: $path failed", error)
                uiState = uiState.copy(
                    isLoading = false,
                    message = uiMapper.failureText(R.string.fm_open_folder_failed, error)
                )
            }
        }
    }

    private fun showDirectory(content: DirectoryContent) {
        val isSameFolder = content.path == currentPath
        Log.d(TAG, "showDirectory: ${content.path}, entries=${content.entries.size}, sameFolder=$isSameFolder")
        currentPath = content.path

        val entries = content.entries.map(uiMapper::toUi).withLocks()
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
            Log.w(TAG, "operation: another one is still running")
            uiState = uiState.copy(message = uiMapper.text(R.string.fm_operation_running))
            return
        }

        uiState = uiState.copy(isBusy = true)
        operationJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val message = try {
                block().also { result -> Log.d(TAG, "operation: done in ${System.currentTimeMillis() - startedAt} ms, message=$result") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "operation: failed", error)
                uiMapper.failureText(failure, error)
            }

            uiState = uiState.copy(isBusy = false, message = message ?: uiState.message)
            loadDirectory(currentPath)
        }
    }

    private class PendingUnlock(
        val paths: List<StoragePath>,
        val entry: FilePasswords.Entry,
        val action: () -> Unit
    )

    @AssistedFactory
    interface Factory {
        fun create(rootDirectory: File, passwords: FilePasswords?): FileManagerViewModel
    }

    private companion object {
        const val KEY_CURRENT_PATH = "file_manager.current_path"
        const val KEY_PENDING_EXPORT = "file_manager.pending_export"
    }
}
