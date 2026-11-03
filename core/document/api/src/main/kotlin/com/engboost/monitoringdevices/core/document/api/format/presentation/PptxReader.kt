package com.engboost.monitoringdevices.core.document.api.format.presentation

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Microsoft PowerPoint 2007+ (.pptx). */
interface PptxReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.PPTX)
}
