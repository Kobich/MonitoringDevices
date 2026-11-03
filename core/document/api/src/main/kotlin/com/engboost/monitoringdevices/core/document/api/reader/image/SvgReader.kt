package com.engboost.monitoringdevices.core.document.api.reader.image

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** Vector images (.svg), shown by the WebView as a picture (scripts in the file don't run). */
interface SvgReader : DocumentReader<HtmlDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.SVG)
}
