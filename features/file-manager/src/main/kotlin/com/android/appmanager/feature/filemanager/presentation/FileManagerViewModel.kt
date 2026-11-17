package com.android.appmanager.feature.filemanager.presentation

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.appmanager.feature.filemanager.FileStorage
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
import com.android.appmanager.feature.filemanager.presentation.model.FileMessageUi
import com.android.appmanager.feature.filemanager.presentation.model.MenuUi
import com.android.appmanager.feature.filemanager.presentation.model.PickerUi
import com.android.appmanager.feature.filemanager.presentation.model.SelectionUi
import com.android.appmanager.feature.filemanager.presentation.model.UiText
import com.android.core.fileaccess.ActionAccess
import com.android.core.fileaccess.FileAccessConfig
import com.android.core.fileaccess.FileAccess
import com.android.core.fileaccess.FileAccessRules
import com.android.core.fileaccess.FileAction
import com.android.core.fileaccess.Password
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Shows the files of [storage] under the administrator's config: actions it leaves out are hidden, protected ones ask
 * for their password each time, files with a message show it instead of opening. The config may change at any time.
 */
@HiltViewModel(assistedFactory = FileManagerViewModel.Factory::class)
internal class FileManagerViewModel @AssistedInject constructor(
    @Assisted private val storage: FileStorage,
    @ApplicationContext context: Context,
    repositoryFactory: FileStorageRepository.Factory,
    rules: FileAccessRules,
    private val uiMapper: FileManagerUiMapper,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), FileManagerActions {

    private val interactor = FileManagerInteractor(repositoryFactory.create(storage.directory(context)))

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

    /** `null` until the first config arrives; actions stay hidden till then. */
    private var config: FileAccessConfig? = null
    private var pendingAction: PendingAction? = null

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
            fileMessage = null,
            menu = MenuUi.None,
            message = null,
            picker = null,
            fileToOpen = null
        )
    )
        private set

    init {
        loadDirectory(currentPath)
        viewModelScope.launch { rules.config.collect { config -> onConfigChanged(config) } }
    }

    /** The first resume comes right after init: the folder it started reading is fresh enough. */
    fun onScreenResumed() {
        if (loadJob?.isActive == true) return
        loadDirectory(currentPath)
    }

    override fun onEntryClick(entry: FileEntryUi) {
        when {
            uiState.isSelectionMode -> toggleSelection(entry)
            entry.isDirectory -> loadDirectory(StoragePath(entry.path))
            else -> {
                logClick("open_file", "list_item", "File")
                // A password is asked by the viewer: it guards the file however it's opened.
                when (val access = config?.access(configPath(entry))) {
                    is FileAccess.Message -> {
                        uiState = uiState.copy(fileMessage = FileMessageUi(entry.name, access.text))
                    }
                    else -> openFile(entry)
                }
            }
        }
    }

    override fun onEntryLongClick(entry: FileEntryUi) {
        if (uiState.isSelectionMode) {
            toggleSelection(entry)
        } else if (uiState.menu.canSelect) {
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

        withAccess(FileAction.DELETE) {
            uiState = uiState.copy(pendingDeletion = uiMapper.deletion(entries))
        }
    }

    override fun onExportSelectedClick() {
        logClick("export", "menu_item", "Export")
        val entries = selectedEntries()
        if (entries.isEmpty()) return

        withAccess(FileAction.EXPORT) { requestExport(entries) }
    }

    private fun requestExport(entries: List<FileEntryUi>) {
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
        logClick("import_files", "menu_item", "Import files")
        withAccess(FileAction.IMPORT) { uiState = uiState.copy(picker = PickerUi.ImportFiles) }
    }

    override fun onImportFolderClick() {
        logClick("import_folder", "menu_item", "Import folder")
        withAccess(FileAction.IMPORT) { uiState = uiState.copy(picker = PickerUi.ImportFolder) }
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
        logClick("delete_confirm", "dialog_button", "Delete")
        val entries = uiState.pendingDeletion?.entries ?: return
        uiState = uiState.copy(pendingDeletion = null, selection = null)

        runOperation(R.string.fm_delete_failed) {
            interactor.delete(entries.map { entry -> StoragePath(entry.path) })
            uiMapper.deletedText(entries)
        }
    }

    override fun onCreateDirectoryClick() {
        if (!uiState.menu.canCreateFolder) return
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
            uiState = uiState.copy(
                createDirectoryDialog = error?.let { CreateDirectoryDialogUi(uiMapper.errorText(it)) }
            )
            null
        }
    }

    override fun onPasswordConfirm(password: String) {
        logClick("password_confirm", "dialog_button", "OK")
        val pending = pendingAction ?: return
        val matches = pending.password.matches(password)
        if (!matches) {
            uiState = uiState.copy(passwordDialog = uiState.passwordDialog?.copy(error = R.string.fm_password_wrong))
            return
        }
        pendingAction = null
        uiState = uiState.copy(passwordDialog = null)
        pending.run()
    }

    override fun onPasswordDismiss() {
        pendingAction = null
        uiState = uiState.copy(passwordDialog = null)
    }

    override fun onFileMessageDismiss() {
        uiState = uiState.copy(fileMessage = null)
    }

    /** Runs [run] if the config has [action], after its password if it has one. */
    private fun withAccess(action: FileAction, run: () -> Unit) {
        when (val access = config?.access(action) ?: ActionAccess.Hidden) {
            ActionAccess.Allowed -> run()
            ActionAccess.Hidden -> Unit
            is ActionAccess.PasswordRequired -> {
                pendingAction = PendingAction(action, access.password, run)
                uiState = uiState.copy(passwordDialog = uiMapper.passwordDialog(action))
            }
        }
    }

    /**
     * The list and the menu follow the new config at once. Dialogs that may ask for what has changed close; an
     * operation already running, or a system picker already open, is finished under the old one.
     */
    private fun onConfigChanged(config: FileAccessConfig) {
        val previous = this.config
        this.config = config

        val menu = MenuUi(
            canImport = config.access(FileAction.IMPORT) != ActionAccess.Hidden,
            canExport = config.access(FileAction.EXPORT) != ActionAccess.Hidden,
            canDelete = config.access(FileAction.DELETE) != ActionAccess.Hidden,
            canCreateFolder = true
        )
        val deleteChanged = previous?.access(FileAction.DELETE) != config.access(FileAction.DELETE)
        pendingAction = null
        uiState = uiState.copy(
            menu = menu,
            entries = uiState.entries.withAccess(),
            selection = uiState.selection?.takeIf { menu.canSelect },
            passwordDialog = null,
            pendingDeletion = uiState.pendingDeletion?.takeIf { !deleteChanged }
        )
    }

    private fun List<FileEntryUi>.withAccess(): List<FileEntryUi> = map { entry ->
        val access = if (entry.isDirectory) null else config?.access(configPath(entry))
        entry.copy(isLocked = access is FileAccess.PasswordRequired, hasMessage = access is FileAccess.Message)
    }

    /** Config paths start at the folder of all storages: `<storage>/<path in it>`. */
    private fun configPath(entry: FileEntryUi): String = "${storage.name}/${entry.path}"

    fun onMessageShown() {
        uiState = uiState.copy(message = null)
    }

    private fun openFile(entry: FileEntryUi) {
        viewModelScope.launch {
            try {
                val file = interactor.fileOf(StoragePath(entry.path))
                uiState = uiState.copy(fileToOpen = file)
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

        val entries = content.entries.map(uiMapper::toUi).withAccess()
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
    private fun runOperation(@StringRes failure: Int, block: suspend () -> UiText?) {
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

    private class PendingAction(
        val action: FileAction,
        val password: Password,
        val run: () -> Unit
    )

    @AssistedFactory
    interface Factory {
        fun create(storage: FileStorage): FileManagerViewModel
    }

    private companion object {
        const val KEY_CURRENT_PATH = "file_manager.current_path"
        const val KEY_PENDING_EXPORT = "file_manager.pending_export"
    }
}
