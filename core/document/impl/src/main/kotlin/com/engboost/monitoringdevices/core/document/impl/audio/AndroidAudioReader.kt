package com.engboost.monitoringdevices.core.document.impl.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.document.AudioDocument
import com.engboost.monitoringdevices.core.document.api.format.audio.AudioReader
import com.engboost.monitoringdevices.core.document.impl.image.BitmapSampling
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Audio metadata with platform APIs: tags and cover from `MediaMetadataRetriever`,
 * sample rate and channels from `MediaExtractor`. Supports what the device decoders support
 * (mp3, wav, aac/m4a, ogg/opus, flac on all current devices).
 */
internal class AndroidAudioReader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AudioReader {

    override suspend fun read(source: DocumentSource, password: String?): AudioDocument = withContext(ioDispatcher) {
        if (source is DocumentSource.LocalFile && !source.file.isFile) {
            throw DocumentReadException(Reason.NOT_FOUND, "${source.name} not found")
        }
        // Metadata is optional: a file whose tags can't be read may still play fine,
        // the player reports a real problem.
        val metadata = runCatching { readMetadata(source) }.getOrNull() ?: Metadata.EMPTY
        val track = readTrackFormat(source)

        AndroidAudioDocument(
            name = source.name,
            source = source,
            durationMs = metadata.durationMs ?: track?.durationUs?.div(1000),
            title = metadata.title,
            artist = metadata.artist,
            album = metadata.album,
            mimeType = metadata.mimeType ?: track?.mimeType,
            sampleRateHz = track?.sampleRateHz,
            channelCount = track?.channelCount,
            bitrateBps = metadata.bitrateBps,
            hasArtwork = metadata.hasArtwork
        )
    }

    private class Metadata(
        val durationMs: Long?,
        val title: String?,
        val artist: String?,
        val album: String?,
        val mimeType: String?,
        val bitrateBps: Int?,
        val hasArtwork: Boolean
    ) {
        companion object {
            val EMPTY = Metadata(null, null, null, null, null, null, hasArtwork = false)
        }
    }

    private class TrackFormat(
        val mimeType: String?,
        val sampleRateHz: Int?,
        val channelCount: Int?,
        val durationUs: Long?
    )

    private fun readMetadata(source: DocumentSource): Metadata {
        return withRetriever(source) { retriever ->
            fun text(key: Int) = retriever.extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }

            Metadata(
                durationMs = text(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                title = text(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = text(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: text(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                album = text(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                mimeType = text(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                bitrateBps = text(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull(),
                hasArtwork = retriever.embeddedPicture != null
            )
        }
    }

    /** First audio track; `null` if the extractor can't open the file (metadata may still work). */
    private fun readTrackFormat(source: DocumentSource): TrackFormat? {
        val extractor = MediaExtractor()
        return try {
            when (source) {
                is DocumentSource.LocalFile -> extractor.setDataSource(source.file.path)
                is DocumentSource.Content -> extractor.setDataSource(context, source.uri, null)
            }
            (0 until extractor.trackCount)
                .map { index -> extractor.getTrackFormat(index) }
                .firstOrNull { format -> format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?.let { format ->
                    TrackFormat(
                        mimeType = format.getString(MediaFormat.KEY_MIME),
                        sampleRateHz = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE),
                        channelCount = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT),
                        durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                            format.getLong(MediaFormat.KEY_DURATION)
                        } else {
                            null
                        }
                    )
                }
        } catch (error: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    private fun MediaFormat.intOrNull(key: String): Int? = if (containsKey(key)) getInteger(key) else null

    private fun <T> withRetriever(source: DocumentSource, block: (MediaMetadataRetriever) -> T): T {
        val retriever = MediaMetadataRetriever()
        try {
            when (source) {
                is DocumentSource.LocalFile -> {
                    if (!source.file.isFile) throw DocumentReadException(Reason.NOT_FOUND, "${source.name} not found")
                    retriever.setDataSource(source.file.path)
                }

                is DocumentSource.Content -> retriever.setDataSource(context, source.uri)
            }
            return block(retriever)
        } catch (error: DocumentReadException) {
            throw error
        } catch (error: IllegalArgumentException) {
            throw DocumentReadException(Reason.UNSUPPORTED_FORMAT, "Can't read ${source.name}", error)
        } catch (error: RuntimeException) {
            throw DocumentReadException(Reason.CORRUPTED, error.message, error)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private inner class AndroidAudioDocument(
        override val name: String,
        override val source: DocumentSource,
        override val durationMs: Long?,
        override val title: String?,
        override val artist: String?,
        override val album: String?,
        override val mimeType: String?,
        override val sampleRateHz: Int?,
        override val channelCount: Int?,
        override val bitrateBps: Int?,
        override val hasArtwork: Boolean
    ) : AudioDocument {
        override val format: DocumentFormat = DocumentFormat.AUDIO

        override suspend fun loadArtwork(maxSizePx: Int): Bitmap? = withContext(ioDispatcher) {
            if (!hasArtwork) return@withContext null
            val bytes = withRetriever(source) { retriever -> retriever.embeddedPicture } ?: return@withContext null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val sample = BitmapSampling.sampleSize(bounds.outWidth, bounds.outHeight, maxSizePx, maxSizePx)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}
