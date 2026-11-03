package com.engboost.monitoringdevices.core.document.api.reader.presentation

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** Apple Keynote (.key). */
interface KeynoteReader : DocumentReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.KEYNOTE)
}
