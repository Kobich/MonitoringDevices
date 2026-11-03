package com.engboost.monitoringdevices.core.document.api.reader.text

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** Markdown (.md): formatted [com.engboost.monitoringdevices.core.document.api.document.HtmlDocument] when possible, otherwise plain [com.engboost.monitoringdevices.core.document.api.document.TextDocument]. */
interface MarkdownReader : DocumentReader<Document> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.MARKDOWN)
}
