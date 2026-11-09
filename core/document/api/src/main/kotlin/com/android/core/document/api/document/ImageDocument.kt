package com.android.core.document.api.document

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.android.core.document.api.DocumentReadException

interface ImageDocument : Document {
    val widthPx: Int
    val heightPx: Int

    val isAnimated: Boolean

    /**
     * Downsampled to fit, with EXIF orientation applied; animated images give their first frame.
     * The caller owns the bitmap.
     */
    @Throws(DocumentReadException::class)
    suspend fun decode(maxWidthPx: Int, maxHeightPx: Int): Bitmap

    /**
     * `null` for a single frame or before Android 9. The drawable is an `Animatable`:
     * the caller starts and stops it.
     */
    @Throws(DocumentReadException::class)
    suspend fun decodeAnimation(maxWidthPx: Int, maxHeightPx: Int): Drawable?
}
