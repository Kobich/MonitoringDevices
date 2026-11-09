package com.android.core.document.api.document

import android.graphics.Bitmap
import com.android.core.document.api.DocumentReadException
import java.io.File

interface AudioDocument : Document {
    val file: File

    val durationMs: Long?
    val title: String?
    val artist: String?
    val album: String?
    val sampleRateHz: Int?
    val channelCount: Int?
    val bitrateBps: Int?

    /** Downsampled to fit [maxSizePx], `null` if there is no cover. The caller owns the bitmap. */
    @Throws(DocumentReadException::class)
    suspend fun loadArtwork(maxSizePx: Int): Bitmap?
}
