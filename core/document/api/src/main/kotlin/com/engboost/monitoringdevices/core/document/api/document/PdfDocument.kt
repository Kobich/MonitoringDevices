package com.engboost.monitoringdevices.core.document.api.document

import android.graphics.Bitmap
import com.engboost.monitoringdevices.core.document.api.DocumentReadException

/** Methods are safe to call concurrently from any coroutine. */
interface PdfDocument : Document {
    val pageCount: Int

    @Throws(DocumentReadException::class)
    suspend fun pageSize(pageIndex: Int): PageSize

    /** On a white background. The caller owns the bitmap. */
    @Throws(DocumentReadException::class)
    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap
}

data class PageSize(
    val widthPt: Int,
    val heightPt: Int
)
