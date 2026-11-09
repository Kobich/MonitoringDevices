package com.android.core.document.api

import com.android.core.document.api.document.Document
import java.io.File

/** Opens files for showing. The implementation comes from `:core:document:impl`. */
interface DocumentReader {
    /**
     * Opens [file]; the format comes from its extension. Heavy work runs off the main thread.
     * The caller must close the returned document.
     *
     * @param password for an encrypted document, after [DocumentReadException.Reason.PASSWORD_REQUIRED].
     * @throws DocumentReadException the only error it throws, with the [DocumentReadException.Reason].
     */
    @Throws(DocumentReadException::class)
    suspend fun read(file: File, password: String? = null): Document
}
