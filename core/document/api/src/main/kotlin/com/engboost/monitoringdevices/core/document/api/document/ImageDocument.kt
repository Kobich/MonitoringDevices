package com.engboost.monitoringdevices.core.document.api.document

import android.graphics.Bitmap
import com.engboost.monitoringdevices.core.document.api.DocumentReadException

/** An image whose size is known; pixels are decoded on demand for the needed resolution. */
interface ImageDocument : Document {
    val widthPx: Int
    val heightPx: Int

    /**
     * Decodes the image downsampled to fit into [maxWidthPx] x [maxHeightPx],
     * applying EXIF orientation. The caller owns the bitmap.
     */
    @Throws(DocumentReadException::class)
    suspend fun decode(maxWidthPx: Int, maxHeightPx: Int): Bitmap
}
