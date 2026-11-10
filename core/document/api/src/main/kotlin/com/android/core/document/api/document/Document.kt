package com.android.core.document.api.document

import com.android.core.document.api.DocumentFormat
import java.io.Closeable

/** The caller must [close] it. */
sealed interface Document : Closeable {
    /** With the extension. */
    val name: String
    val format: DocumentFormat

    override fun close() = Unit
}
