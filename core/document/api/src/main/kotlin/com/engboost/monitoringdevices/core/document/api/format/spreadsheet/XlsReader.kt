package com.engboost.monitoringdevices.core.document.api.format.spreadsheet

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Legacy Microsoft Excel 97-2003 (.xls), one page per sheet. */
interface XlsReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.XLS)
}
