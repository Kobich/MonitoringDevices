package com.engboost.monitoringdevices.core.document.api.format.spreadsheet

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** OpenDocument spreadsheet (.ods, flat .fods), one page per sheet. */
interface OdsReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.ODS)
}
