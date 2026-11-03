package com.engboost.monitoringdevices.core.document.api.html

import com.engboost.monitoringdevices.core.document.api.DocumentType
import java.io.File

/**
 * Result of [HtmlDocumentRenderer.render].
 *
 * @property directory root of the output; every page and its resources (images, styles)
 * are inside it, so it can be served as a single folder, e.g. with `WebViewAssetLoader`.
 * @property pages at least one. Text documents usually have a single page,
 * spreadsheets one page per sheet.
 */
data class RenderedDocument(
    val type: DocumentType,
    val directory: File,
    val pages: List<RenderedPage>
)

data class RenderedPage(
    /** Sheet / slide / document name to show in tabs. */
    val title: String,
    /** HTML file inside [RenderedDocument.directory]. */
    val file: File
)
