package com.engboost.monitoringdevices.core.document.api.document

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.engboost.monitoringdevices.core.document.api.DocumentReadException

/** An image whose size is known; pixels are decoded on demand for the needed resolution. */
interface ImageDocument : Document {
    val widthPx: Int
    val heightPx: Int

    /** GIF or animated WebP; see [decodeAnimation]. */
    val isAnimated: Boolean

    /**
     * Decodes the image downsampled to fit into [maxWidthPx] x [maxHeightPx],
     * applying EXIF orientation. Animated images give their first frame. The caller owns the bitmap.
     */
    @Throws(DocumentReadException::class)
    suspend fun decode(maxWidthPx: Int, maxHeightPx: Int): Bitmap

    /**
     * Animated drawable (`android.graphics.drawable.Animatable`) fitting into the limits, or `null`
     * when the image has a single frame or the device can't animate it (Android 9+ is needed).
     * The caller starts and stops the animation.
     */
    @Throws(DocumentReadException::class)
    suspend fun decodeAnimation(maxWidthPx: Int, maxHeightPx: Int): Drawable?
}
