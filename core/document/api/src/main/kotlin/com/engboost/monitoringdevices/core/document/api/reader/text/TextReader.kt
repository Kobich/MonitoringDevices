package com.engboost.monitoringdevices.core.document.api.reader.text

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** Plain text (.txt, .md, .json, .xml, .log); the charset is detected. */
interface TextReader : DocumentReader<TextDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.TEXT)
}
