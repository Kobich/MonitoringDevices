package com.engboost.monitoringdevices.core.document.api.reader.drawing

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** OpenDocument drawing (.odg, .fodg). */
interface OdgReader : DocumentReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.ODG)
}
