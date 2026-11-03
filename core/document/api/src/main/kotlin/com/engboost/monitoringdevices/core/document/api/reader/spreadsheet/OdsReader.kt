package com.engboost.monitoringdevices.core.document.api.reader.spreadsheet

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** OpenDocument spreadsheet (.ods, flat .fods), one page per sheet. */
interface OdsReader : DocumentReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.ODS)
}
