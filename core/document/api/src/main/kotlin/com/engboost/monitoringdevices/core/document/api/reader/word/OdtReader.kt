package com.engboost.monitoringdevices.core.document.api.reader.word

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** OpenDocument text (.odt, flat .fodt). */
interface OdtReader : DocumentReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.ODT)
}
