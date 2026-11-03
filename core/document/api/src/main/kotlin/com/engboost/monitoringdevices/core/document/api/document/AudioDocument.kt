package com.engboost.monitoringdevices.core.document.api.document

import android.graphics.Bitmap
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource

/** Audio file metadata; playback is up to the viewer, which plays [source]. */
interface AudioDocument : Document {
    val source: DocumentSource

    /** `null` when the container doesn't tell. */
    val durationMs: Long?
    val title: String?
    val artist: String?
    val album: String?
    val mimeType: String?
    val sampleRateHz: Int?
    val channelCount: Int?
    val bitrateBps: Int?
    val hasArtwork: Boolean

    /** Embedded cover downsampled to fit [maxSizePx], or `null`. The caller owns the bitmap. */
    @Throws(DocumentReadException::class)
    suspend fun loadArtwork(maxSizePx: Int): Bitmap?
}
