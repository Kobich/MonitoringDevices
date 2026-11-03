package com.engboost.monitoringdevices.feature.viewer.impl.presentation.model

import androidx.compose.runtime.Immutable
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument

@Immutable
internal data class DocumentViewerUiState(
    val title: String,
    val content: ViewerContent,
    /** `null` when the file has no chart variants to compare. */
    val chartRendering: ChartRendering?,
    val passwordDialog: PasswordDialogUi?
)

@Immutable
internal sealed interface ViewerContent {
    data object Loading : ViewerContent

    data class Html(
        val document: HtmlDocument,
        val selectedPageIndex: Int
    ) : ViewerContent

    /** The document was read, but this viewer can't show its kind yet. */
    data class NotImplemented(val kind: String) : ViewerContent

    data class Error(val message: String) : ViewerContent
}

@Immutable
internal data class PasswordDialogUi(
    val error: String?
)
