package com.engboost.monitoringdevices.core.document.api.text

import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource

interface TextReader {
    /**
     * Reads a plain text file. The charset is detected (BOM / UTF-8 validation),
     * the content is cut after [maxChars] characters.
     */
    @Throws(DocumentReadException::class)
    suspend fun read(source: DocumentSource, maxChars: Int = DEFAULT_MAX_CHARS): TextDocument

    companion object {
        const val DEFAULT_MAX_CHARS = 1_000_000
    }
}

data class TextDocument(
    val text: String,
    /** Name of the detected charset, e.g. `UTF-8`. */
    val charsetName: String,
    /** `true` if the file is longer than the requested limit. */
    val isTruncated: Boolean
)
