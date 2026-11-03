package com.engboost.monitoringdevices.core.document.api

import com.engboost.monitoringdevices.core.document.api.document.Document
import java.io.File

/**
 * Opens any supported file:
 *
 * ```
 * reader.read(file).use { document ->
 *     when (document) {
 *         is PdfDocument -> ...
 *         is HtmlDocument -> ...
 *         is TextDocument -> ...
 *         is ImageDocument -> ...
 *         is AudioDocument -> ...
 *     }
 * }
 * ```
 *
 * The format comes from the file extension. Heavy work runs off the main thread.
 * The caller owns the returned document and must close it.
 *
 * @throws DocumentReadException with a [DocumentReadException.Reason].
 */
interface DocumentReader {
    @Throws(DocumentReadException::class)
    suspend fun read(file: File, password: String? = null): Document

    @Throws(DocumentReadException::class)
    suspend fun read(path: String, password: String? = null): Document = read(File(path), password)
}
