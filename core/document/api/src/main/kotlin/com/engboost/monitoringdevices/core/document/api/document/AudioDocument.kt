package com.engboost.monitoringdevices.core.document.api.document

import android.graphics.Bitmap
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import java.io.File

/** Audio file metadata; playback is up to the viewer, which plays [file]. */
interface AudioDocument : Document {
    val file: File

    /** `null` when the container doesn't tell. */
    val durationMs: Long?
    val title: String?
    val artist: String?
    val album: String?
    val sampleRateHz: Int?
    val channelCount: Int?
    val bitrateBps: Int?
    val hasArtwork: Boolean

    /** Embedded cover downsampled to fit [maxSizePx], or `null`. The caller owns the bitmap. */
    @Throws(DocumentReadException::class)
    suspend fun loadArtwork(maxSizePx: Int): Bitmap?
}
