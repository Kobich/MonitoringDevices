package com.engboost.monitoringdevices.core.document.impl

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.DocumentReader
import com.engboost.monitoringdevices.core.document.api.document.Document
import java.io.File

/** Finds the reader for the file extension and delegates to it. */
internal class CompositeDocumentReader(
    private val readers: List<FormatReader<Document>>
) : DocumentReader {

    init {
        val formats = readers.flatMap { reader -> reader.formats }
        require(formats.size == formats.toSet().size) { "Several readers registered for one format" }
    }

    override suspend fun read(file: File, password: String?): Document {
        if (!file.isFile) throw DocumentReadException(Reason.NOT_FOUND, "${file.name} not found")

        val format = DocumentFormat.of(file)
        val reader = readers.firstOrNull { reader -> format in reader.formats }
            ?: throw DocumentReadException(Reason.UNSUPPORTED_FORMAT, "Can't open ${file.name}")
        return reader.read(file, password)
    }
}
