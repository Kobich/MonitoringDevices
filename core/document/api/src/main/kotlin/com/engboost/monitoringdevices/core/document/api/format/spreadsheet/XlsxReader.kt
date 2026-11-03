package com.engboost.monitoringdevices.core.document.api.format.spreadsheet

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Microsoft Excel 2007+ (.xlsx), one page per sheet. */
interface XlsxReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.XLSX)
}
