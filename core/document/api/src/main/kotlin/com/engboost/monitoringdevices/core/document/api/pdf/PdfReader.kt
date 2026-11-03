package com.engboost.monitoringdevices.core.document.api.pdf

import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource

interface PdfReader {
    /**
     * Opens a PDF for page rendering. The caller owns the returned document and must close it.
     *
     * @param password for encrypted documents; may be ignored by implementations
     * that don't support encryption (they throw [DocumentReadException.Reason.PASSWORD_REQUIRED]).
     */
    @Throws(DocumentReadException::class)
    suspend fun open(source: DocumentSource, password: String? = null): PdfDocument
}
