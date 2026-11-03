package com.engboost.monitoringdevices.core.document.impl.text

import android.util.Log
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.reader.text.MarkdownReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter

/**
 * Markdown formatted by OpenDocument.core; plain text when the library isn't available
 * ([converter] is `null` below API 26) or fails, so `.md` files always open.
 */
internal class AndroidMarkdownReader(
    private val converter: OdrHtmlConverter?,
    private val textReader: AndroidTextReader
) : MarkdownReader {

    override suspend fun read(source: DocumentSource, options: ReadOptions): Document {
        if (converter != null) {
            try {
                return converter.convert(
                    source = source,
                    format = DocumentFormat.MARKDOWN,
                    password = null,
                    variant = VARIANT
                )
            } catch (error: DocumentReadException) {
                if (error.reason == DocumentReadException.Reason.NOT_FOUND) throw error
                Log.w(TAG, "Formatted Markdown failed for ${source.name}, showing plain text", error)
            }
        }
        return textReader.readText(source, DocumentFormat.MARKDOWN)
    }

    private companion object {
        const val TAG = "AndroidMarkdownReader"
        const val VARIANT = "markdown"
    }
}
