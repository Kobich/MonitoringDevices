package com.android.appmanager.feature.documentviewer.presentation.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.android.core.document.api.document.Document

@Immutable
internal data class DocumentViewerUiState(
    val title: String,
    val content: ViewerContent,
    val passwordDialog: PasswordDialogUi?
)

/** The document stays owned (and closed) by the ViewModel. */
@Immutable
internal sealed interface ViewerContent {
    data object Loading : ViewerContent

    /** [selectedPageIndex] is the open tab of an `HtmlDocument`. */
    data class Loaded(val document: Document, val selectedPageIndex: Int = 0) : ViewerContent

    data class Error(@StringRes val title: Int, val details: String? = null) : ViewerContent
}

@Immutable
internal data class PasswordDialogUi(
    val isWrongPassword: Boolean
)

@Immutable
internal data class AudioPlaybackUi(
    val isReady: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val isFailed: Boolean
)
