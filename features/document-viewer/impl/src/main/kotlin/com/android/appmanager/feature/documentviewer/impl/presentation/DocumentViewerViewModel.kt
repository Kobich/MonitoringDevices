package com.android.appmanager.feature.documentviewer.impl.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.appmanager.feature.documentviewer.impl.R
import com.android.appmanager.feature.documentviewer.impl.domain.player.AudioPlayerFactory
import com.android.appmanager.feature.documentviewer.impl.presentation.model.DocumentViewerUiState
import com.android.appmanager.feature.documentviewer.impl.presentation.model.PasswordDialogUi
import com.android.appmanager.feature.documentviewer.impl.presentation.model.ViewerContent
import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import com.android.core.document.api.DocumentReader
import com.android.core.document.api.document.AudioDocument
import com.android.core.document.api.document.Document
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel(assistedFactory = DocumentViewerViewModel.Factory::class)
internal class DocumentViewerViewModel @AssistedInject constructor(
    @Assisted private val file: File,
    private val reader: DocumentReader,
    private val audioPlayerFactory: AudioPlayerFactory
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(file: File): DocumentViewerViewModel
    }

    private var loadJob: Job? = null
    private var openedDocument: Document? = null
    private var password: String? = null

    var uiState by mutableStateOf(
        DocumentViewerUiState(title = file.name, content = ViewerContent.Loading, passwordDialog = null)
    )
        private set

    /** Present while an audio document is shown. */
    var audioPlayback by mutableStateOf<AudioPlayback?>(null)
        private set

    init {
        load()
    }

    fun onPageSelected(index: Int) {
        val content = uiState.content as? ViewerContent.Loaded ?: return
        uiState = uiState.copy(content = content.copy(selectedPageIndex = index))
    }

    fun onPasswordSubmit(value: String) {
        password = value
        uiState = uiState.copy(passwordDialog = null)
        load()
    }

    fun onPasswordDismiss() {
        uiState = uiState.copy(passwordDialog = null).withError(R.string.viewer_password_protected)
    }

    fun onRetryClick() {
        load()
    }

    fun onScreenStopped() {
        audioPlayback?.pause()
    }

    private fun load() {
        loadJob?.cancel()
        uiState = uiState.copy(content = ViewerContent.Loading)

        loadJob = viewModelScope.launch {
            try {
                show(reader.read(file, password))
            } catch (error: CancellationException) {
                throw error
            } catch (error: DocumentReadException) {
                handleReadError(error)
            } catch (error: Exception) {
                uiState = uiState.withError(R.string.viewer_error_io, error.message)
            }
        }
    }

    private fun show(document: Document) {
        closeOpenedDocument()
        openedDocument = document
        if (document is AudioDocument) {
            audioPlayback = AudioPlayback(
                player = audioPlayerFactory.create(document.file),
                scope = viewModelScope,
                knownDurationMs = document.durationMs ?: 0
            )
        }
        uiState = uiState.copy(content = ViewerContent.Loaded(document))
    }

    private fun handleReadError(error: DocumentReadException) {
        uiState = when (error.reason) {
            Reason.PASSWORD_REQUIRED -> uiState.copy(passwordDialog = PasswordDialogUi(isWrongPassword = false))
            Reason.INVALID_PASSWORD -> uiState.copy(passwordDialog = PasswordDialogUi(isWrongPassword = true))
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
        audioPlayback?.release()
        audioPlayback = null
        openedDocument?.close()
        openedDocument = null
    }

    override fun onCleared() {
        closeOpenedDocument()
    }
}
