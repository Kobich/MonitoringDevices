package com.android.core.document.api.document

import com.android.core.document.api.DocumentFormat
import java.io.Closeable

/**
 * An opened file, returned by [com.android.core.document.api.DocumentReader.read]. Sealed: handle every
 * kind with an exhaustive `when`. The caller must [close] it.
 */
sealed interface Document : Closeable {
    /** File name with its extension, for titles. */
    val name: String
    val format: DocumentFormat

    override fun close() = Unit
}
