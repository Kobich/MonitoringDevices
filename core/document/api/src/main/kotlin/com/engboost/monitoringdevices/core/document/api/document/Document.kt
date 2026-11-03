package com.engboost.monitoringdevices.core.document.api.document

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import java.io.Closeable

/**
 * Result of [com.engboost.monitoringdevices.core.document.api.DocumentReader.read].
 * Sealed, so the viewer handles every kind with an exhaustive `when`:
 *
 * ```
 * when (document) {
 *     is PdfDocument -> PdfPages(document)
 *     is HtmlDocument -> HtmlPages(document)
 *     is TextDocument -> PlainText(document)
 *     is ImageDocument -> Picture(document)
 *     is AudioDocument -> Player(document)
 * }
 * ```
 *
 * The caller owns the document and must [close] it.
 */
sealed interface Document : Closeable {
    val name: String
    val format: DocumentFormat

    override fun close() = Unit
}
