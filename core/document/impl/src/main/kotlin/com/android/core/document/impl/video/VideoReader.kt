package com.android.core.document.impl.video

import android.media.MediaMetadataRetriever
import android.util.Log
import com.android.core.document.api.DocumentFormat
import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import com.android.core.document.api.document.Document
import com.android.core.document.api.document.VideoDocument
import com.android.core.document.impl.FormatReader
import com.android.core.document.impl.audio.AudioReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "VideoReader"

/** A container without a video track (e.g. an `.mp4` with sound only) opens as audio. */
internal class VideoReader(
    private val audioReader: AudioReader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FormatReader<Document> {
    override val formats = setOf(
        DocumentFormat.MP4,
        DocumentFormat.M4V,
        DocumentFormat.MOV,
        DocumentFormat.THREE_GP,
        DocumentFormat.WEBM,
        DocumentFormat.MKV
    )

    override suspend fun read(file: File, format: DocumentFormat): Document {
        val tracks = withContext(ioDispatcher) { readTracks(file) }
        Log.d(TAG, "read: ${file.name} hasVideo=${tracks.hasVideo}, hasAudio=${tracks.hasAudio}")
        return when {
            tracks.hasVideo -> VideoDocument(name = file.name, format = format, file = file)
            tracks.hasAudio -> audioReader.read(file, format)
            else -> throw DocumentReadException(Reason.CORRUPTED, "No video or audio in ${file.name}")
        }
    }

    private class Tracks(val hasVideo: Boolean, val hasAudio: Boolean)

    private fun readTracks(file: File): Tracks {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.path)
            return Tracks(
                hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes",
                hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            )
        } catch (error: RuntimeException) {
            Log.w(TAG, "readTracks: ${file.name} unreadable", error)
            // MediaMetadataRetriever reports unreadable files with IllegalArgumentException / RuntimeException.
            throw DocumentReadException(Reason.CORRUPTED, error.message, error)
        } finally {
            runCatching { retriever.release() }
        }
    }
}
