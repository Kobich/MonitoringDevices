package com.engboost.monitoringdevices.core.document.api.format

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReader
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.document.Document

/**
 * Reader of particular formats (`PdfReader`, `DocxReader`, ...), each declares its formats and
 * the kind of [Document] it produces. [DocumentReader] is built from them; use one directly only
 * when you need exactly that format and its typed result.
 */
interface FormatReader<out D : Document> {
    val formats: Set<DocumentFormat>

    @Throws(DocumentReadException::class)
    suspend fun read(source: DocumentSource, password: String? = null): D
}
