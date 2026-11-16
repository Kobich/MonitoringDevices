package com.android.appmanager.feature.documentviewer.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.android.appmanager.feature.documentviewer.presentation.model.AudioPlaybackUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** [scope] must run on the main thread, like the player. */
internal class AudioPlayback(
    private val player: Player,
    private val scope: CoroutineScope,
    knownDurationMs: Long
) {
    var state by mutableStateOf(
        AudioPlaybackUi(isReady = false, isPlaying = false, positionMs = 0, durationMs = knownDurationMs, isFailed = false)
    )
        private set

    private var progressJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    state = state.copy(isReady = true)
                    update()
                }

                Player.STATE_ENDED -> {
                    // Pause first: seeking an ended player that should play starts it again.
                    player.pause()
                    player.seekTo(0)
                    update(positionMs = 0)
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
            update()
        }

        override fun onPlayerError(error: PlaybackException) {
            state = state.copy(isFailed = true)
        }
    }

    init {
        player.addListener(listener)
    }

    fun onPlayPauseClick() {
        if (player.isPlaying) pause() else play()
    }

    fun onSeek(positionMs: Long) {
        player.seekTo(positionMs)
        update(positionMs = positionMs)
    }

    fun pause() {
        player.pause()
    }

    fun release() {
        stopProgressUpdates()
        player.removeListener(listener)
        player.release()
    }

    private fun play() {
        player.play()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                update()
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun update(positionMs: Long = player.currentPosition) {
        state = state.copy(
            isPlaying = player.isPlaying,
            positionMs = positionMs,
            durationMs = player.duration.takeIf { it > 0 } ?: state.durationMs
        )
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 200L
    }
}
