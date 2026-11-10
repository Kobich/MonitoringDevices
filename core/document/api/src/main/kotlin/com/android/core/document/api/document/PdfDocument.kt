package com.android.core.document.api.document

import android.graphics.Bitmap
import com.android.core.document.api.DocumentReadException

/** Safe to call concurrently. */
interface PdfDocument : Document {
    val pageCount: Int

    @Throws(DocumentReadException::class)
    suspend fun pageSize(pageIndex: Int): PageSize

    /** On a white background. The caller owns the bitmap. */
    @Throws(DocumentReadException::class)
    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap
}

/** In PDF points, 1/72 of an inch. */
data class PageSize(
    val widthPt: Int,
    val heightPt: Int
)
