package com.android.core.document.api.document

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.android.core.document.api.DocumentReadException

interface ImageDocument : Document {
    /** A GIF or an animated WebP: try [decodeAnimation] before [decode]. */
    val isAnimated: Boolean

    /**
     * Downsampled to fit, with the EXIF rotation applied; an animated image gives its first frame.
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
