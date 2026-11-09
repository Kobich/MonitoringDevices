package com.android.appmanager.feature.documentviewer.data.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.android.appmanager.feature.documentviewer.domain.player.AudioPlayer
import com.android.appmanager.feature.documentviewer.domain.player.AudioPlayerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Must be used from the main thread: `MediaPlayer` callbacks arrive on the creating looper. */
internal class MediaPlayerAudioPlayer(
    private val file: File
) : AudioPlayer {
    private val player = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
    }
    private var isPrepared = false

    override val durationMs: Long
        get() = if (isPrepared) player.duration.toLong().coerceAtLeast(0) else 0

    override val positionMs: Long
        get() = if (isPrepared) player.currentPosition.toLong() else 0

    override val isPlaying: Boolean
        get() = isPrepared && player.isPlaying

    override suspend fun prepare() {
        player.setDataSource(file.path)
        suspendCancellableCoroutine { continuation ->
            player.setOnPreparedListener {
                isPrepared = true
                if (continuation.isActive) continuation.resume(Unit)
            }
            player.setOnErrorListener { _, what, extra ->
                if (continuation.isActive) {
                    continuation.resumeWithException(IOException("Can't play the file (error $what/$extra)"))
                }
                true
            }
            continuation.invokeOnCancellation { runCatching { player.reset() } }
            player.prepareAsync()
        }
    }

    override fun play() {
        if (isPrepared) player.start()
    }

    override fun pause() {
        if (isPlaying) player.pause()
    }

    override fun seekTo(positionMs: Long) {
        if (isPrepared) player.seekTo(positionMs.toInt())
    }

    override fun setOnCompletionListener(listener: () -> Unit) {
        player.setOnCompletionListener { listener() }
    }

    override fun release() {
        isPrepared = false
        player.release()
    }
}

internal class MediaPlayerAudioPlayerFactory @Inject constructor() : AudioPlayerFactory {
    override fun create(file: File): AudioPlayer = MediaPlayerAudioPlayer(file)
}
