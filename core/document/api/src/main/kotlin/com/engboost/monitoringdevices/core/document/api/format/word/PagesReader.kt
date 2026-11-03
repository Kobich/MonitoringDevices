package com.engboost.monitoringdevices.core.document.api.format.word

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Apple Pages (.pages). */
interface PagesReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.PAGES)
}
