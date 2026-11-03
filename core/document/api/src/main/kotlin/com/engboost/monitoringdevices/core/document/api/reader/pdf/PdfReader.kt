package com.engboost.monitoringdevices.core.document.api.reader.pdf

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.PdfDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** PDF, rendered page by page into bitmaps. */
interface PdfReader : DocumentReader<PdfDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.PDF)
}
