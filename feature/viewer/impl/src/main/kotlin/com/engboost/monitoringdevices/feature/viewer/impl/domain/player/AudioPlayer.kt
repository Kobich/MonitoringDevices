package com.engboost.monitoringdevices.feature.viewer.impl.domain.player

import com.engboost.monitoringdevices.core.document.api.DocumentSource

/**
 * Playback of a single audio file while the viewer is open. Implemented with the platform
 * `MediaPlayer` now; Media3 (ExoPlayer) is the option for background playback,
 * notification controls and audio focus.
 */
internal interface AudioPlayer {
    val durationMs: Long
    val positionMs: Long
    val isPlaying: Boolean

    /** Suspends until the player can start; throws if the file can't be played. */
    suspend fun prepare()

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setOnCompletionListener(listener: () -> Unit)
    fun release()
}

internal fun interface AudioPlayerFactory {
    fun create(source: DocumentSource): AudioPlayer
}
