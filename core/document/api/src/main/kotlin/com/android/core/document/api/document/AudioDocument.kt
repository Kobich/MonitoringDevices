package com.android.core.document.api.document

import android.graphics.Bitmap
import com.android.core.document.api.DocumentReadException
import java.io.File

/**
 * An audio file with its metadata; the viewer plays [file]. Every tag is `null` when the file doesn't
 * have it: a file without readable tags still opens.
 */
interface AudioDocument : Document {
    val file: File

    val durationMs: Long?
    val title: String?

    /** The artist, or the album artist when the track has none. */
    val artist: String?
    val album: String?
    val sampleRateHz: Int?
    val channelCount: Int?
    val bitrateBps: Int?

    /** Downsampled to fit [maxSizePx], `null` if there is no cover. The caller owns the bitmap. */
    @Throws(DocumentReadException::class)
    suspend fun loadArtwork(maxSizePx: Int): Bitmap?
}
