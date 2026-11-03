package com.engboost.monitoringdevices.core.document.api

import com.engboost.monitoringdevices.core.document.api.document.Document
import java.io.File

/**
 * THE entry point of the module: give it a file, get a [Document].
 *
 * ```
 * reader.read("/data/user/0/app/files/report.docx").use { document ->
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
 * The format is detected by the file name (and MIME type for `content://`), the right
 * reader is chosen inside. Heavy work runs off the main thread.
 */
interface DocumentReader {
    /** Every format [read] can open on this device. */
    val supportedFormats: Set<DocumentFormat>

    /** Checks the name only, the file isn't touched. */
    fun canRead(path: String): Boolean

    /**
     * Opens a file by absolute path, `file://` or `content://` URI.
     * The caller owns the returned document and must close it.
     *
     * @param password for encrypted documents.
     * @throws DocumentReadException with a [DocumentReadException.Reason].
     */
    @Throws(DocumentReadException::class)
    suspend fun read(path: String, password: String? = null): Document

    @Throws(DocumentReadException::class)
    suspend fun read(file: File, password: String? = null): Document = read(file.path, password)

    @Throws(DocumentReadException::class)
    suspend fun read(source: DocumentSource, password: String? = null): Document
}
