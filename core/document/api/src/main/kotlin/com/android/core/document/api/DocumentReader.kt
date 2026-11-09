package com.android.core.document.api

import com.android.core.document.api.document.Document
import java.io.File

/**
 * The format comes from the file extension. Heavy work runs off the main thread.
 * The caller must close the returned document.
 */
interface DocumentReader {
    @Throws(DocumentReadException::class)
    suspend fun read(file: File, password: String? = null): Document
}
