package com.engboost.monitoringdevices.core.document.api.format.spreadsheet

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Apple Numbers (.numbers), one page per sheet. */
interface NumbersReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.NUMBERS)
}
