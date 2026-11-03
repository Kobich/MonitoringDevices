package com.engboost.monitoringdevices.core.document.api.format.image

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Vector images (.svg), shown by the WebView as a picture (scripts in the file don't run). */
interface SvgReader : FormatReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.SVG)
}
