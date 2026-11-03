package com.engboost.monitoringdevices.core.document.api.image

import android.graphics.Bitmap
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource

interface ImageReader {
    /**
     * Decodes an image downsampled to fit into [maxWidthPx] x [maxHeightPx],
     * applying EXIF orientation. The caller owns the bitmap.
     */
    @Throws(DocumentReadException::class)
    suspend fun decode(source: DocumentSource, maxWidthPx: Int, maxHeightPx: Int): Bitmap

    /** Original dimensions without decoding pixels. */
    @Throws(DocumentReadException::class)
    suspend fun size(source: DocumentSource): ImageSize
}

data class ImageSize(
    val widthPx: Int,
    val heightPx: Int
)
