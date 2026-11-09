package com.android.core.document.impl.audio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import com.android.core.document.api.DocumentFormat
import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import com.android.core.document.api.document.AudioDocument
import com.android.core.document.impl.FormatReader
import com.android.core.document.impl.image.BitmapSampling
import com.android.core.document.impl.mapReadErrors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * All of it is optional: a file whose tags can't be read still opens,
 * the player reports real playback problems.
 */
internal class AudioReader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FormatReader<AudioDocument> {
    override val formats = setOf(
        DocumentFormat.MP3,
        DocumentFormat.WAV,
        DocumentFormat.M4A,
        DocumentFormat.AAC,
        DocumentFormat.OGG,
        DocumentFormat.OPUS,
        DocumentFormat.FLAC
    )

    override suspend fun read(file: File, format: DocumentFormat, password: String?): AudioDocument = withContext(ioDispatcher) {
        val tags = runCatching { readTags(file) }.getOrNull() ?: Tags()
        val track = runCatching { readTrack(file) }.getOrNull()

        FileAudioDocument(
            file = file,
            format = format,
            durationMs = tags.durationMs ?: track?.durationUs?.div(1000),
            title = tags.title,
            artist = tags.artist,
            album = tags.album,
            sampleRateHz = track?.sampleRateHz,
            channelCount = track?.channelCount,
            bitrateBps = tags.bitrateBps
        )
    }

    private class Tags(
        val durationMs: Long? = null,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val bitrateBps: Int? = null
    )

    private class Track(
        val sampleRateHz: Int?,
        val channelCount: Int?,
        val durationUs: Long?
    )

    private fun readTags(file: File): Tags = withRetriever(file) { retriever ->
        fun text(key: Int) = retriever.extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }

        Tags(
            durationMs = text(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
            title = text(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = text(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: text(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
            album = text(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            bitrateBps = text(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
        )
    }

    private fun readTrack(file: File): Track? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.path)
            val format = (0 until extractor.trackCount)
                .map { index -> extractor.getTrackFormat(index) }
                .firstOrNull { format -> format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: return null
            return Track(
                sampleRateHz = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE),
                channelCount = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT),
                durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else null
            )
        } finally {
            extractor.release()
        }
    }

    private fun MediaFormat.intOrNull(key: String): Int? = if (containsKey(key)) getInteger(key) else null

    private fun <T> withRetriever(file: File, block: (MediaMetadataRetriever) -> T): T {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.path)
            return block(retriever)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private inner class FileAudioDocument(
        override val file: File,
        override val format: DocumentFormat,
        override val durationMs: Long?,
        override val title: String?,
        override val artist: String?,
        override val album: String?,
        override val sampleRateHz: Int?,
        override val channelCount: Int?,
        override val bitrateBps: Int?
    ) : AudioDocument {
        override val name: String = file.name

        override suspend fun loadArtwork(maxSizePx: Int): Bitmap? = withContext(ioDispatcher) {
            val bytes = try {
                withRetriever(file) { retriever -> retriever.embeddedPicture } ?: return@withContext null
            } catch (error: RuntimeException) {
                // MediaMetadataRetriever reports unreadable files with IllegalArgumentException / RuntimeException.
                throw DocumentReadException(Reason.CORRUPTED, error.message, error)
            }

            mapReadErrors {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val sample = BitmapSampling.sampleSize(bounds.outWidth, bounds.outHeight, maxSizePx, maxSizePx)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
    }
}
