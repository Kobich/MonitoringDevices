package com.engboost.monitoringdevices.core.document.api.format.pdf

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.PdfDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** PDF, rendered page by page into bitmaps. */
interface PdfReader : FormatReader<PdfDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.PDF)
}
