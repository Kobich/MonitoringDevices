package com.engboost.monitoringdevices.core.document.api.reader

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.Document

/**
 * Single entry point for opening any supported file: delegates to the reader
 * registered for the file format.
 */
class UniversalDocumentReader(
    val readers: List<DocumentReader<Document>>
) : DocumentReader<Document> {

    init {
        val duplicates = readers
            .flatMap { reader -> reader.formats }
            .groupingBy { format -> format }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
        require(duplicates.isEmpty()) { "Several readers registered for $duplicates" }
    }

    override val formats: Set<DocumentFormat> = readers.flatMapTo(mutableSetOf()) { reader -> reader.formats }

    fun readerFor(source: DocumentSource): DocumentReader<Document>? {
        val format = DocumentFormat.of(source) ?: return null
        return readers.firstOrNull { reader -> format in reader.formats }
    }

    override suspend fun read(source: DocumentSource, options: ReadOptions): Document {
        val reader = readerFor(source)
            ?: throw DocumentReadException(
                reason = DocumentReadException.Reason.UNSUPPORTED_FORMAT,
                message = "No reader for ${source.name}"
            )
        return reader.read(source, options)
    }
}
