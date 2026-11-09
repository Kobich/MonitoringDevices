package com.android.core.document.api.document

import android.graphics.Bitmap
import com.android.core.document.api.DocumentReadException

/** A PDF rendered page by page. Methods are safe to call concurrently from any coroutine. */
interface PdfDocument : Document {
    val pageCount: Int

    /** Size of the page at [pageIndex] (from 0), to reserve its place before it is rendered. */
    @Throws(DocumentReadException::class)
    suspend fun pageSize(pageIndex: Int): PageSize

    /** On a white background. The caller owns the bitmap. */
    @Throws(DocumentReadException::class)
    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap
}

/** Page size in PDF points, 1/72 of an inch. */
data class PageSize(
    val widthPt: Int,
    val heightPt: Int
)
