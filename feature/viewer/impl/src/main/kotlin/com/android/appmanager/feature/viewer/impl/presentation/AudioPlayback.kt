package com.android.appmanager.feature.viewer.impl.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.appmanager.feature.viewer.impl.domain.player.AudioPlayer
import com.android.appmanager.feature.viewer.impl.presentation.model.AudioPlaybackUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Playback of one audio document; [scope] must run on the main thread, like the player. */
internal class AudioPlayback(
    private val player: AudioPlayer,
    private val scope: CoroutineScope,
    knownDurationMs: Long
) {
    var state by mutableStateOf(
        AudioPlaybackUi(isReady = false, isPlaying = false, positionMs = 0, durationMs = knownDurationMs, isFailed = false)
    )
        private set

    private var progressJob: Job? = null

    init {
        player.setOnCompletionListener {
            stopProgressUpdates()
            player.seekTo(0)
            update(positionMs = 0)
        }
        scope.launch {
            try {
                player.prepare()
                state = state.copy(isReady = true)
                update()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state = state.copy(isFailed = true)
            }
        }
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
        stopProgressUpdates()
        update()
    }

    fun release() {
        stopProgressUpdates()
        player.release()
    }

    private fun play() {
        player.play()
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

    private fun update(positionMs: Long = player.positionMs) {
        state = state.copy(
            isPlaying = player.isPlaying,
            positionMs = positionMs,
            durationMs = player.durationMs.takeIf { it > 0 } ?: state.durationMs
        )
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 200L
    }
}
