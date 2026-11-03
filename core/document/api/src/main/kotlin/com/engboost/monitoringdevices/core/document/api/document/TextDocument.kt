package com.engboost.monitoringdevices.core.document.api.document

import com.engboost.monitoringdevices.core.document.api.DocumentFormat

data class TextDocument(
    override val name: String,
    override val format: DocumentFormat,
    val text: String,
    /** Detected charset, e.g. `UTF-8`. */
    val charsetName: String,
    /** `true` if the file was longer than the reader's limit and [text] is cut. */
    val isTruncated: Boolean
) : Document
