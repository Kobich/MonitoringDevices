package com.android.core.document.impl

import android.util.Log
import com.android.core.document.api.DocumentFormat
import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import com.android.core.document.api.DocumentReader
import com.android.core.document.api.document.Document
import java.io.File

private const val TAG = "DocumentReader"

internal class CompositeDocumentReader(
    private val readers: List<FormatReader<Document>>
) : DocumentReader {

    init {
        val formats = readers.flatMap { reader -> reader.formats }
        require(formats.size == formats.toSet().size) { "Several readers registered for one format" }
    }

    override suspend fun read(file: File, password: String?): Document {
        Log.d(TAG, "read: ${file.path}, size=${file.length()}, withPassword=${password != null}")
        if (!file.isFile) {
            Log.w(TAG, "read: ${file.name} not found")
            throw DocumentReadException(Reason.NOT_FOUND, "${file.name} not found")
        }

        val format = DocumentFormat.of(file)
        val reader = readers.firstOrNull { reader -> format in reader.formats }
        if (format == null || reader == null) {
            Log.w(TAG, "read: ${file.name} unsupported, format=$format")
            throw DocumentReadException(Reason.UNSUPPORTED_FORMAT, "Can't open ${file.name}")
        }
        Log.d(TAG, "read: ${file.name} format=$format reader=${reader::class.simpleName}")

        val startedAt = System.currentTimeMillis()
        try {
            val document = mapReadErrors { reader.read(file, format, password) }
            Log.d(TAG, "read: ${file.name} opened as ${document::class.simpleName} in ${System.currentTimeMillis() - startedAt} ms")
            return document
        } catch (error: DocumentReadException) {
            Log.w(TAG, "read: ${file.name} failed, reason=${error.reason}, message=${error.message}", error)
            throw error
        }
    }
}
