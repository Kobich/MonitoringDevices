package com.android.appmanager.feature.documentviewer.impl.data.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** The player must be used and released on the main thread. */
internal class VideoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun create(file: File): Player {
        return ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
        }
    }
}
