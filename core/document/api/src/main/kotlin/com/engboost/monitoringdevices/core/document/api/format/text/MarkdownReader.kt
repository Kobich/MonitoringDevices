package com.engboost.monitoringdevices.core.document.api.format.text

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Markdown (.md): formatted [com.engboost.monitoringdevices.core.document.api.document.HtmlDocument] when possible, otherwise plain [com.engboost.monitoringdevices.core.document.api.document.TextDocument]. */
interface MarkdownReader : FormatReader<Document> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.MARKDOWN)
}
