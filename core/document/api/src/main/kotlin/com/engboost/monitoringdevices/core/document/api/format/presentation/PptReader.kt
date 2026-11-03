package com.engboost.monitoringdevices.core.document.api.format.presentation

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Legacy Microsoft PowerPoint 97-2003 (.ppt). */
interface PptReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.PPT)
}
