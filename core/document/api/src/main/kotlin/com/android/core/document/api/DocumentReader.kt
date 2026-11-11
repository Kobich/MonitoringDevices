package com.android.core.document.api

import com.android.core.document.api.document.Document
import java.io.File

/** Implemented in `:core:document:impl`. */
interface DocumentReader {
    /**
     * The format comes from the extension, not the content. Main-safe. The caller closes the document.
     *
     * @throws DocumentReadException the only exception, here and in the documents' methods.
     */
    @Throws(DocumentReadException::class)
    suspend fun read(file: File): Document
}
