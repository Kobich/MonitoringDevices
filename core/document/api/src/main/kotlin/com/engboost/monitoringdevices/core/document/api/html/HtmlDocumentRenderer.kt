package com.engboost.monitoringdevices.core.document.api.html

import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.DocumentType

/**
 * Converts office documents into self-contained HTML for a WebView, keeping
 * the original look (fonts, spacing, tables, images).
 *
 * Output lives in a cache owned by the implementation: callers only read it and must not
 * modify or delete files. Rendering the same unchanged source again returns the cached result.
 */
interface HtmlDocumentRenderer {
    fun canRender(type: DocumentType): Boolean

    /**
     * @param password for encrypted documents. Without it an encrypted document fails with
     * [DocumentReadException.Reason.PASSWORD_REQUIRED], with a wrong one with
     * [DocumentReadException.Reason.INVALID_PASSWORD].
     */
    @Throws(DocumentReadException::class)
    suspend fun render(source: DocumentSource, password: String? = null): RenderedDocument

    /** Removes all rendered output, e.g. on low storage. */
    suspend fun clearCache()
}
