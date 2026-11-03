package com.engboost.monitoringdevices.core.document.impl.text

import android.util.Log
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.impl.FormatReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import java.io.File

/**
 * Markdown formatted by OpenDocument.core; plain text when the library isn't available
 * ([converter] is `null` below API 26) or fails, so `.md` files always open.
 */
internal class MarkdownReader(
    private val converter: OdrHtmlConverter?,
    private val textReader: TextReader
) : FormatReader<Document> {
    override val formats = setOf(DocumentFormat.MARKDOWN)

    override suspend fun read(file: File, password: String?): Document {
        if (converter != null) {
            try {
                return converter.convert(file, DocumentFormat.MARKDOWN, password = null, variant = VARIANT)
            } catch (error: DocumentReadException) {
                Log.w(TAG, "Formatted Markdown failed for ${file.name}, showing plain text", error)
            }
        }
        return textReader.readText(file, DocumentFormat.MARKDOWN)
    }

    private companion object {
        const val TAG = "MarkdownReader"
        const val VARIANT = "markdown"
    }
}
