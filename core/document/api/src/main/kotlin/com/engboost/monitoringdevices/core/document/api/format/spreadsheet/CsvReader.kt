package com.engboost.monitoringdevices.core.document.api.format.spreadsheet

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Comma-separated values (.csv), shown as a table. */
interface CsvReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.CSV)
}
