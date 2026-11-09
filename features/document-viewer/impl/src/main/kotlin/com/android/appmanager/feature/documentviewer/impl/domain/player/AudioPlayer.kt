package com.android.appmanager.feature.documentviewer.impl.domain.player

import java.io.File

internal interface AudioPlayer {
    val durationMs: Long
    val positionMs: Long
    val isPlaying: Boolean

    /** Throws if the file can't be played. */
    suspend fun prepare()

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setOnCompletionListener(listener: () -> Unit)
    fun release()
}

internal fun interface AudioPlayerFactory {
    fun create(file: File): AudioPlayer
}
