package com.engboost.monitoringdevices.core.document.api.document

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import java.io.File

/**
 * Office document converted to self-contained HTML that keeps the original look.
 * Shown in a WebView.
 *
 * Files live in a cache owned by the reader implementation: read them, don't modify or delete.
 *
 * @property directory root of the output; pages and their resources (images, styles) are
 * inside it, so it can be served as a single folder, e.g. with `WebViewAssetLoader`.
 * @property pages at least one: a text document usually has one, a spreadsheet one per sheet.
 */
data class HtmlDocument(
    override val name: String,
    override val format: DocumentFormat,
    val directory: File,
    val pages: List<HtmlPage>
) : Document

data class HtmlPage(
    /** Sheet / slide / document name for tabs. */
    val title: String,
    /** HTML file inside [HtmlDocument.directory]. */
    val file: File
)
