package com.android.core.document.impl.text

import android.util.Log
import com.android.core.document.api.DocumentFormat
import com.android.core.document.api.document.Document
import com.android.core.document.impl.FormatReader
import com.android.core.document.impl.office.OdrHtmlConverter
import java.io.File
import java.io.IOException

/** Plain text when OpenDocument.core isn't available (below API 26) or fails. */
internal class MarkdownReader(
    private val converter: OdrHtmlConverter?,
    private val textReader: TextReader
) : FormatReader<Document> {
    override val formats = setOf(DocumentFormat.MARKDOWN)

    override suspend fun read(file: File, format: DocumentFormat, password: String?): Document {
        if (converter != null) {
            try {
                return converter.convert(file, format, password = null, variant = VARIANT)
            } catch (error: IOException) {
                // DocumentReadException is an IOException too: any failure falls back to plain text.
                Log.w(TAG, "Formatted Markdown failed for ${file.name}, showing plain text", error)
            }
        }
        return textReader.read(file, format, password = null)
    }

    private companion object {
        const val TAG = "MarkdownReader"
        const val VARIANT = "markdown"
    }
}
