package com.engboost.monitoringdevices.core.document.api.pdf

import android.graphics.Bitmap
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import java.io.Closeable

/**
 * An opened PDF. Methods are safe to call from any coroutine; implementations serialize
 * access when the underlying renderer allows only one page at a time.
 */
interface PdfDocument : Closeable {
    val pageCount: Int

    /** Page size in PDF points (1/72 inch), useful to reserve space before rendering. */
    @Throws(DocumentReadException::class)
    suspend fun pageSize(pageIndex: Int): PdfPageSize

    /**
     * Renders a page scaled to [widthPx], keeping the aspect ratio, on a white background.
     * The caller owns the bitmap and may recycle it.
     */
    @Throws(DocumentReadException::class)
    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap

    /** Plain text of a page, or `null` if the implementation can't extract text. */
    @Throws(DocumentReadException::class)
    suspend fun pageText(pageIndex: Int): String?
}

data class PdfPageSize(
    val widthPt: Int,
    val heightPt: Int
)
