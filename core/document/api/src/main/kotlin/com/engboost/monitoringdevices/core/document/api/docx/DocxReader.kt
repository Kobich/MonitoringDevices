package com.engboost.monitoringdevices.core.document.api.docx

import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.richtext.RichTextDocument

interface DocxReader {
    /** Parses an Office Open XML document (`.docx`). Legacy binary `.doc` is not supported. */
    @Throws(DocumentReadException::class)
    suspend fun read(source: DocumentSource): RichTextDocument
}
