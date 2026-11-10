package com.android.core.document.api.document

import com.android.core.document.api.DocumentFormat
import java.io.File

/**
 * Office files, Markdown and SVG. The files live in the reader's cache: don't modify or delete them.
 *
 * @property directory holds the pages and their resources, so it can be served as one folder.
 * @property pages at least one; a spreadsheet has one per sheet.
 */
data class HtmlDocument(
    override val name: String,
    override val format: DocumentFormat,
    val directory: File,
    val pages: List<HtmlPage>
) : Document

/** @property file inside [HtmlDocument.directory]. */
data class HtmlPage(
    val title: String,
    val file: File
)
