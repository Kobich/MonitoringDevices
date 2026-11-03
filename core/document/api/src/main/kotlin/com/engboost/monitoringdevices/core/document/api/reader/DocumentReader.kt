package com.engboost.monitoringdevices.core.document.api.reader

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.Document

/**
 * Base of all readers. Each concrete reader (`PdfReader`, `DocxReader`, `XlsxReader`, ...)
 * declares its formats and the kind of [Document] it produces.
 *
 * UI code normally uses [UniversalDocumentReader], which picks the right reader by format.
 */
interface DocumentReader<out D : Document> {
    val formats: Set<DocumentFormat>

    fun canRead(source: DocumentSource): Boolean = DocumentFormat.of(source) in formats

    /** Heavy work runs off the main thread inside the implementation. */
    @Throws(DocumentReadException::class)
    suspend fun read(source: DocumentSource, options: ReadOptions = ReadOptions()): D
}
