package com.android.appmanager.feature.documentviewer.presentation.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.android.core.document.api.document.Document

@Immutable
internal data class DocumentViewerUiState(
    val title: String,
    val content: ViewerContent,
    /** The config has export and the file is open. */
    val canOpenInOtherApp: Boolean = false,
    val passwordDialog: ViewerPasswordUi? = null,
    /** One-shot: the config took the open file away, leave the viewer. Cleared by `onCloseHandled`. */
    val isCloseRequested: Boolean = false,
    /** One-shot: hand the file to another app. Cleared by `onOpenInOtherAppHandled`. */
    val isOpenInOtherAppRequested: Boolean = false
)

@Immutable
internal data class ViewerPasswordUi(
    @StringRes val title: Int,
    @StringRes val error: Int?
)

/** The document stays owned (and closed) by the ViewModel. */
@Immutable
internal sealed interface ViewerContent {
    /** Also while waiting for the config. */
    data object Loading : ViewerContent

    /** The file opens with a password, the dialog asks for it. */
    data object Locked : ViewerContent

    /** The administrator's message, shown instead of the file. */
    data class Unavailable(val message: String) : ViewerContent

    /** [selectedPageIndex] is the open tab of an `HtmlDocument`. */
    data class Loaded(val document: Document, val selectedPageIndex: Int = 0) : ViewerContent

    data class Error(@StringRes val title: Int, val details: String? = null) : ViewerContent
}

@Immutable
internal data class AudioPlaybackUi(
    val isReady: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val isFailed: Boolean
)
