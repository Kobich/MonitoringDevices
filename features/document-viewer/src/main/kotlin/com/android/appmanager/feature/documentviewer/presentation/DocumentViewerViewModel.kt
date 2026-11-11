package com.android.appmanager.feature.documentviewer.presentation

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
import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import com.android.core.document.api.DocumentReader
import com.android.core.document.api.document.AudioDocument
import com.android.core.document.api.document.Document
import com.android.core.document.api.document.VideoDocument
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "DocumentViewer"

@HiltViewModel(assistedFactory = DocumentViewerViewModel.Factory::class)
internal class DocumentViewerViewModel @AssistedInject constructor(
    @Assisted private val file: File,
    private val reader: DocumentReader,
    private val playerFactory: PlayerFactory
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(file: File): DocumentViewerViewModel
    }

    private var loadJob: Job? = null
    private var openedDocument: Document? = null

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
        load()
    }

    fun onPageSelected(index: Int) {
        val content = uiState.content as? ViewerContent.Loaded ?: return
        uiState = uiState.copy(content = content.copy(selectedPageIndex = index))
    }

    fun onRetryClick() {
        load()
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
}
