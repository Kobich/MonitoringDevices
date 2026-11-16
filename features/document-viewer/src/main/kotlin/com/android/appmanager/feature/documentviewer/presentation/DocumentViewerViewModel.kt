package com.android.appmanager.feature.documentviewer.presentation

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.android.appmanager.feature.documentviewer.R
import com.android.appmanager.feature.documentviewer.data.player.PlayerFactory
import com.android.appmanager.feature.documentviewer.presentation.model.DocumentViewerUiState
import com.android.appmanager.feature.documentviewer.presentation.model.ViewerContent
import com.android.appmanager.feature.documentviewer.presentation.model.ViewerPasswordUi
import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import com.android.core.document.api.DocumentReader
import com.android.core.document.api.document.AudioDocument
import com.android.core.document.api.document.Document
import com.android.core.document.api.document.VideoDocument
import com.android.core.fileaccess.Access
import com.android.core.fileaccess.AppFilesRoot
import com.android.core.fileaccess.FileAccessConfig
import com.android.core.fileaccess.FileAccessRules
import com.android.core.fileaccess.FileAction
import com.android.core.fileaccess.Password
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "DocumentViewer"

/**
 * Opens [file] under the administrator's config: a file with a message shows it instead, one with a password asks for
 * it each time the viewer opens. If the config later takes the open file away, the viewer closes.
 */
@HiltViewModel(assistedFactory = DocumentViewerViewModel.Factory::class)
internal class DocumentViewerViewModel @AssistedInject constructor(
    @Assisted private val file: File,
    @ApplicationContext private val context: Context,
    private val reader: DocumentReader,
    private val playerFactory: PlayerFactory,
    private val rules: FileAccessRules
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(file: File): DocumentViewerViewModel
    }

    private var loadJob: Job? = null
    private var openedDocument: Document? = null

    /** What the config asked of the file when it was let through; `null` until then. */
    private var openedWith: Access? = null
    private var exportAccess: Access = Access.Hidden
    private var pendingPassword: PendingPassword? = null

    var uiState by mutableStateOf(
        DocumentViewerUiState(title = file.name, content = ViewerContent.Loading)
    )
        private set

    var audioPlayback by mutableStateOf<AudioPlayback?>(null)
        private set

    /** Here, not in the screen, so playback survives rotation. */
    var videoPlayer by mutableStateOf<Player?>(null)
        private set

    init {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { AppFilesRoot.pathOf(file, AppFilesRoot.directory(context)) }
            rules.config.collect { config -> onConfigChanged(config, path) }
        }
    }

    fun onPageSelected(index: Int) {
        val content = uiState.content as? ViewerContent.Loaded ?: return
        uiState = uiState.copy(content = content.copy(selectedPageIndex = index))
    }

    fun onRetryClick() {
        logClick("retry", "button", "Retry")
        if (openedWith != null) load()
    }

    fun onOpenInOtherAppClick() {
        logClick("open_in_other_app", "icon_button", "Open in another app")
        when (val access = exportAccess) {
            Access.Allowed, is Access.Message -> uiState = uiState.copy(isOpenInOtherAppRequested = true)
            Access.Hidden -> Log.w(TAG, "open in other app: export isn't available")
            is Access.PasswordRequired -> askPassword(PendingPassword.Export(access.password))
        }
    }

    fun onOpenInOtherAppHandled() {
        uiState = uiState.copy(isOpenInOtherAppRequested = false)
    }

    fun onPasswordConfirm(password: String) {
        logClick("password_confirm", "dialog_button", "OK")
        val pending = pendingPassword ?: return
        val matches = pending.password.matches(password)
        Log.d(TAG, "password: ${pending::class.simpleName}, matches=$matches")
        if (!matches) {
            uiState = uiState.copy(passwordDialog = uiState.passwordDialog?.copy(error = R.string.viewer_password_wrong))
            return
        }
        pendingPassword = null
        uiState = uiState.copy(passwordDialog = null)
        when (pending) {
            is PendingPassword.OpenFile -> open(Access.PasswordRequired(pending.password))
            is PendingPassword.Export -> uiState = uiState.copy(isOpenInOtherAppRequested = true)
        }
    }

    /** Without the file's password there's nothing to show: the viewer closes. */
    fun onPasswordDismiss() {
        val pending = pendingPassword
        pendingPassword = null
        uiState = uiState.copy(passwordDialog = null, isCloseRequested = pending is PendingPassword.OpenFile)
    }

    fun onCloseHandled() {
        uiState = uiState.copy(isCloseRequested = false)
    }

    private fun onConfigChanged(config: FileAccessConfig, path: String?) {
        val access = path?.let { config.access(it) } ?: Access.Allowed
        val previousExport = exportAccess
        exportAccess = config.access(FileAction.EXPORT)
        Log.d(TAG, "config: file=${access::class.simpleName}, export=${exportAccess::class.simpleName}")

        if (pendingPassword is PendingPassword.Export && exportAccess != previousExport) {
            pendingPassword = null
            uiState = uiState.copy(passwordDialog = null)
        }

        val openedWith = openedWith
        when {
            openedWith == null -> {
                pendingPassword = null
                uiState = uiState.copy(passwordDialog = null)
                when (access) {
                    Access.Allowed, Access.Hidden -> open(Access.Allowed)
                    is Access.Message -> uiState = uiState.copy(content = ViewerContent.Unavailable(access.text))
                    is Access.PasswordRequired -> {
                        uiState = uiState.copy(content = ViewerContent.Locked)
                        askPassword(PendingPassword.OpenFile(access.password))
                    }
                }
            }
            // A new password, or a message, while the file is open: it shouldn't stay on screen.
            access != Access.Allowed && access != openedWith -> {
                Log.d(TAG, "config: ${file.name} is no longer open to this viewer, closing")
                loadJob?.cancel()
                closeOpenedDocument()
                uiState = uiState.copy(content = ViewerContent.Loading, canOpenInOtherApp = false, isCloseRequested = true)
                return
            }
        }
        uiState = uiState.copy(canOpenInOtherApp = this.openedWith != null && exportAccess != Access.Hidden)
    }

    private fun open(access: Access) {
        openedWith = access
        uiState = uiState.copy(canOpenInOtherApp = exportAccess != Access.Hidden)
        load()
    }

    private fun askPassword(pending: PendingPassword) {
        pendingPassword = pending
        val title = when (pending) {
            is PendingPassword.OpenFile -> R.string.viewer_password_file
            is PendingPassword.Export -> R.string.viewer_password_export
        }
        uiState = uiState.copy(passwordDialog = ViewerPasswordUi(title = title, error = null))
    }

    fun onScreenStopped() {
        Log.d(TAG, "screen stopped: pausing playback")
        audioPlayback?.pause()
        videoPlayer?.pause()
    }

    private fun load() {
        loadJob?.cancel()
        uiState = uiState.copy(content = ViewerContent.Loading)
        Log.d(TAG, "load: ${file.path}")

        loadJob = viewModelScope.launch {
            try {
                show(reader.read(file))
            } catch (error: CancellationException) {
                throw error
            } catch (error: DocumentReadException) {
                handleReadError(error)
            } catch (error: Exception) {
                Log.e(TAG, "load: ${file.name} failed unexpectedly", error)
                uiState = uiState.withError(R.string.viewer_error_io, error.message)
            }
        }
    }

    private fun show(document: Document) {
        closeOpenedDocument()
        Log.d(TAG, "show: ${document.name} as ${document::class.simpleName}, format=${document.format}")
        openedDocument = document
        if (document is AudioDocument) {
            audioPlayback = AudioPlayback(
                player = playerFactory.create(document.file),
                scope = viewModelScope,
                knownDurationMs = document.durationMs ?: 0
            )
        }
        if (document is VideoDocument) {
            videoPlayer = playerFactory.create(document.file)
        }
        uiState = uiState.copy(content = ViewerContent.Loaded(document))
    }

    private fun handleReadError(error: DocumentReadException) {
        Log.w(TAG, "load: ${file.name} failed, reason=${error.reason}, message=${error.message}")
        uiState = when (error.reason) {
            Reason.NOT_FOUND -> uiState.withError(R.string.viewer_error_not_found)
            Reason.UNSUPPORTED_FORMAT -> uiState.withError(R.string.viewer_error_unsupported, error.details())
            Reason.CORRUPTED -> uiState.withError(R.string.viewer_error_corrupted)
            Reason.TOO_LARGE -> uiState.withError(R.string.viewer_error_too_large)
            Reason.IO_ERROR -> uiState.withError(R.string.viewer_error_io, error.details())
        }
    }

    private fun DocumentViewerUiState.withError(@StringRes title: Int, details: String? = null) =
        copy(content = ViewerContent.Error(title, details))

    /** The message, unless it only repeats the reason. */
    private fun DocumentReadException.details(): String? = message?.takeIf { it.isNotBlank() && it != reason.name }

    private fun closeOpenedDocument() {
        openedDocument?.let { document -> Log.d(TAG, "close: ${document.name}") }
        audioPlayback?.release()
        audioPlayback = null
        videoPlayer?.release()
        videoPlayer = null
        openedDocument?.close()
        openedDocument = null
    }

    override fun onCleared() {
        closeOpenedDocument()
    }

    private sealed interface PendingPassword {
        val password: Password

        class OpenFile(override val password: Password) : PendingPassword

        class Export(override val password: Password) : PendingPassword
    }
}
