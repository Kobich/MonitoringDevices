package com.engboost.monitoringdevices.feature.viewer.impl.presentation.model

import androidx.compose.runtime.Immutable
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.core.document.api.document.AudioDocument
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument
import com.engboost.monitoringdevices.core.document.api.document.PdfDocument
import com.engboost.monitoringdevices.core.document.api.document.TextDocument

@Immutable
internal data class DocumentViewerUiState(
    val title: String,
    val content: ViewerContent,
    /** `null` when the file has no chart variants to compare. */
    val chartRendering: ChartRendering?,
    val passwordDialog: PasswordDialogUi?,
    /** Present while an audio document is shown. */
    val audioPlayback: AudioPlaybackUi?
)

/** One case per document kind; the documents stay owned (and closed) by the ViewModel. */
@Immutable
internal sealed interface ViewerContent {
    data object Loading : ViewerContent

    data class Html(val document: HtmlDocument, val selectedPageIndex: Int) : ViewerContent

    data class Pdf(val document: PdfDocument) : ViewerContent

    data class Text(val document: TextDocument) : ViewerContent

    data class Image(val document: ImageDocument) : ViewerContent

    data class Audio(val document: AudioDocument) : ViewerContent

    data class Error(val message: String) : ViewerContent
}

@Immutable
internal data class PasswordDialogUi(
    val error: String?
)

@Immutable
internal data class AudioPlaybackUi(
    val isReady: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val error: String?
)
