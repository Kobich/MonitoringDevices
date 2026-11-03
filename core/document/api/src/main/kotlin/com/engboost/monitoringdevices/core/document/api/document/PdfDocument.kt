package com.engboost.monitoringdevices.core.document.api.document

import android.graphics.Bitmap
import com.engboost.monitoringdevices.core.document.api.DocumentReadException

/**
 * An opened PDF, rendered page by page. Methods are safe to call from any coroutine;
 * implementations serialize access when the renderer allows only one open page.
 */
interface PdfDocument : Document {
    val pageCount: Int

    /** Page size in PDF points (1/72 inch), to reserve space before rendering. */
    @Throws(DocumentReadException::class)
    suspend fun pageSize(pageIndex: Int): PageSize

    /** Renders a page scaled to [widthPx] on a white background. The caller owns the bitmap. */
    @Throws(DocumentReadException::class)
    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap

    /** Plain text of a page, or `null` if the implementation can't extract text. */
    @Throws(DocumentReadException::class)
    suspend fun pageText(pageIndex: Int): String?
}

data class PageSize(
    val widthPt: Int,
    val heightPt: Int
)
