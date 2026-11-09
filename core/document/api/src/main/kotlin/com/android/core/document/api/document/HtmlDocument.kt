package com.android.core.document.api.document

import com.android.core.document.api.DocumentFormat
import java.io.File

/**
 * A document converted to HTML for a WebView: office files, Markdown, SVG. Files live in a cache owned
 * by the reader: read them, don't modify or delete.
 *
 * @property directory pages and their resources are inside it, so it can be served as one folder.
 * @property pages at least one: a spreadsheet has one per sheet.
 */
data class HtmlDocument(
    override val name: String,
    override val format: DocumentFormat,
    val directory: File,
    val pages: List<HtmlPage>
) : Document

/**
 * @property title sheet, slide or document name, for tabs.
 * @property file HTML file inside [HtmlDocument.directory].
 */
data class HtmlPage(
    val title: String,
    val file: File
)
