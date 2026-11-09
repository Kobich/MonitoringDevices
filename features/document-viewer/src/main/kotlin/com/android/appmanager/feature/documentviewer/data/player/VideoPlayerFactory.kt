package com.android.appmanager.feature.documentviewer.data.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val TAG = "VideoPlayer"

/** The player must be used and released on the main thread. */
internal class VideoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun create(file: File): Player {
        Log.d(TAG, "create: ${file.name}")
        return ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "playback error for ${file.name}: ${error.errorCodeName}", error)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d(TAG, "state ${file.name}: $playbackState (1 idle, 2 buffering, 3 ready, 4 ended)")
                }
            })
            setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
        }
    }
}
