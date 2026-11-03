package com.engboost.monitoringdevices.feature.viewer.impl.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.core.document.api.document.AudioDocument
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument
import com.engboost.monitoringdevices.core.document.api.document.PdfDocument
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.feature.viewer.impl.domain.interactor.OpenDocumentInteractor
import com.engboost.monitoringdevices.feature.viewer.impl.domain.player.AudioPlayer
import com.engboost.monitoringdevices.feature.viewer.impl.domain.player.AudioPlayerFactory
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.AudioPlaybackUi
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.DocumentViewerUiState
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.PasswordDialogUi
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.ViewerContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel(assistedFactory = DocumentViewerViewModel.Factory::class)
internal class DocumentViewerViewModel @AssistedInject constructor(
    @Assisted private val file: File,
    private val interactor: OpenDocumentInteractor,
    private val audioPlayerFactory: AudioPlayerFactory
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(file: File): DocumentViewerViewModel
    }

    private var loadJob: Job? = null
    private var openedDocument: Document? = null
    private var password: String? = null

    private var audioPlayer: AudioPlayer? = null
    private var progressJob: Job? = null

    var uiState by mutableStateOf(
        DocumentViewerUiState(
            title = file.name,
            content = ViewerContent.Loading,
            chartRendering = ChartRendering.AS_IS.takeIf { interactor.hasChartVariants(file) },
            passwordDialog = null,
            audioPlayback = null
        )
    )
        private set

    init {
        load()
    }

    // region Document

    fun onChartRenderingSelected(chartRendering: ChartRendering) {
        if (uiState.chartRendering == null || uiState.chartRendering == chartRendering) return
        uiState = uiState.copy(chartRendering = chartRendering)
        load()
    }

    fun onPageSelected(index: Int) {
        val content = uiState.content as? ViewerContent.Html ?: return
        uiState = uiState.copy(content = content.copy(selectedPageIndex = index))
    }

    fun onPasswordSubmit(value: String) {
        password = value
        uiState = uiState.copy(passwordDialog = null)
        load()
    }

    fun onPasswordDismiss() {
        uiState = uiState.copy(
            passwordDialog = null,
            content = ViewerContent.Error("The document is protected with a password")
        )
    }

    fun onRetryClick() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        uiState = uiState.copy(content = ViewerContent.Loading)
        val chartRendering = uiState.chartRendering ?: ChartRendering.AS_IS

        loadJob = viewModelScope.launch {
            try {
                val document = interactor.open(file, chartRendering, password)
                replaceOpenedDocument(document)
                show(document)
            } catch (error: CancellationException) {
                throw error
            } catch (error: DocumentReadException) {
                handleReadError(error)
            } catch (error: Exception) {
                showError(error.toDisplayMessage())
            }
        }
    }

    private fun show(document: Document) {
        // Exhaustive: a new document kind must be handled here.
        val content = when (document) {
            is HtmlDocument -> ViewerContent.Html(document = document, selectedPageIndex = 0)
            is PdfDocument -> ViewerContent.Pdf(document)
            is TextDocument -> ViewerContent.Text(document)
            is ImageDocument -> ViewerContent.Image(document)
            is AudioDocument -> ViewerContent.Audio(document)
        }
        uiState = uiState.copy(content = content)
        if (document is AudioDocument) startAudio(document)
    }

    private fun handleReadError(error: DocumentReadException) {
        when (error.reason) {
            Reason.PASSWORD_REQUIRED -> uiState = uiState.copy(passwordDialog = PasswordDialogUi(error = null))
            Reason.INVALID_PASSWORD -> uiState = uiState.copy(passwordDialog = PasswordDialogUi(error = "Wrong password"))
            Reason.NOT_FOUND -> showError("File not found")
            Reason.UNSUPPORTED_FORMAT -> showError(error.message?.takeIf { it.isNotBlank() } ?: "This file type isn't supported")
            Reason.CORRUPTED -> showError("The file is damaged or has an unknown structure")
            Reason.TOO_LARGE -> showError("The file is too large")
            Reason.IO_ERROR -> showError("Can't read the file: ${error.toDisplayMessage()}")
        }
    }

    private fun showError(message: String) {
        uiState = uiState.copy(content = ViewerContent.Error(message))
    }

    private fun replaceOpenedDocument(document: Document) {
        releaseAudio()
        openedDocument?.takeIf { it !== document }?.close()
        openedDocument = document
    }

    // endregion

    // region Audio

    fun onPlayPauseClick() {
        val player = audioPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            stopProgressUpdates()
        } else {
            player.play()
            startProgressUpdates()
        }
        updatePlayback()
    }

    fun onSeek(positionMs: Long) {
        audioPlayer?.seekTo(positionMs)
        updatePlayback(positionMs = positionMs)
    }

    /** The screen went to background: don't keep playing behind the user's back. */
    fun onScreenStopped() {
        val player = audioPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            stopProgressUpdates()
            updatePlayback()
        }
    }

    private fun startAudio(document: AudioDocument) {
        val player = audioPlayerFactory.create(document.source)
        audioPlayer = player
        uiState = uiState.copy(
            audioPlayback = AudioPlaybackUi(
                isReady = false,
                isPlaying = false,
                positionMs = 0,
                durationMs = document.durationMs ?: 0,
                error = null
            )
        )
        player.setOnCompletionListener {
            stopProgressUpdates()
            player.seekTo(0)
            updatePlayback(positionMs = 0)
        }
        viewModelScope.launch {
            try {
                player.prepare()
                uiState = uiState.copy(audioPlayback = uiState.audioPlayback?.copy(isReady = true))
                updatePlayback()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                uiState = uiState.copy(audioPlayback = uiState.audioPlayback?.copy(error = error.toDisplayMessage()))
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                updatePlayback()
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updatePlayback(positionMs: Long? = null) {
        val player = audioPlayer ?: return
        val current = uiState.audioPlayback ?: return
        uiState = uiState.copy(
            audioPlayback = current.copy(
                isPlaying = player.isPlaying,
                positionMs = positionMs ?: player.positionMs,
                durationMs = player.durationMs.takeIf { it > 0 } ?: current.durationMs
            )
        )
    }

    private fun releaseAudio() {
        stopProgressUpdates()
        audioPlayer?.release()
        audioPlayer = null
        uiState = uiState.copy(audioPlayback = null)
    }

    // endregion

    override fun onCleared() {
        stopProgressUpdates()
        audioPlayer?.release()
        audioPlayer = null
        openedDocument?.close()
        openedDocument = null
    }

    private fun Throwable.toDisplayMessage(): String {
        return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 200L
    }
}
