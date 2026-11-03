package com.engboost.monitoringdevices.core.document.impl

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReader
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.format.FormatReader
import com.engboost.monitoringdevices.core.document.impl.source.DocumentSourceFiles

/** [DocumentReader] that dispatches to the [FormatReader] registered for the file format. */
internal class CompositeDocumentReader(
    private val readers: List<FormatReader<Document>>,
    private val sourceFiles: DocumentSourceFiles
) : DocumentReader {

    init {
        val duplicates = readers
            .flatMap { reader -> reader.formats }
            .groupingBy { format -> format }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
        require(duplicates.isEmpty()) { "Several readers registered for $duplicates" }
    }

    override val supportedFormats: Set<DocumentFormat> =
        readers.flatMapTo(mutableSetOf()) { reader -> reader.formats }

    override fun canRead(path: String): Boolean {
        return DocumentFormat.of(sourceFiles.sourceOf(path)) in supportedFormats
    }

    override suspend fun read(path: String, password: String?): Document {
        return read(sourceFiles.sourceOf(path), password)
    }

    override suspend fun read(source: DocumentSource, password: String?): Document {
        val format = DocumentFormat.of(source)
        val reader = readers.firstOrNull { reader -> format in reader.formats }
            ?: throw DocumentReadException(
                reason = DocumentReadException.Reason.UNSUPPORTED_FORMAT,
                message = "No reader for ${source.name}"
            )
        return reader.read(source, password)
    }
}
