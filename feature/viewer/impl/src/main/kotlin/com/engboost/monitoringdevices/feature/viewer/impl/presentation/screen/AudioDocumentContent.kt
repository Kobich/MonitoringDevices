package com.engboost.monitoringdevices.feature.viewer.impl.presentation.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.core.commonui.icons.AppIcons
import com.engboost.monitoringdevices.core.commonui.icons.FileKind
import com.engboost.monitoringdevices.core.commonui.icons.FileTypeIcon
import com.engboost.monitoringdevices.core.document.api.document.AudioDocument
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.AudioPlaybackUi
import java.util.Locale

@Composable
internal fun AudioDocumentContent(
    document: AudioDocument,
    playback: AudioPlaybackUi?,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val artwork by produceState<Bitmap?>(initialValue = null, document) {
        value = runCatching { document.loadArtwork(maxSizePx = ARTWORK_SIZE_PX) }.getOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            val cover = artwork
            if (cover != null) {
                Image(
                    bitmap = cover.asImageBitmap(),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                FileTypeIcon(kind = FileKind.AUDIO, size = 160.dp)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = document.title ?: document.name,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            listOfNotNull(document.artist, document.album).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        PlaybackControls(
            playback = playback,
            fallbackDurationMs = document.durationMs ?: 0,
            onPlayPauseClick = onPlayPauseClick,
            onSeek = onSeek
        )

        technicalInfo(document).takeIf { it.isNotEmpty() }?.let { info ->
            Text(
                text = info,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    playback: AudioPlaybackUi?,
    fallbackDurationMs: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val durationMs = playback?.durationMs?.takeIf { it > 0 } ?: fallbackDurationMs
    // While the thumb is dragged the slider shows the drag position, not the player's.
    var dragPositionMs by remember { mutableStateOf<Float?>(null) }
    val positionMs = dragPositionMs ?: (playback?.positionMs ?: 0L).toFloat()

    Column(
        modifier = Modifier.widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = positionMs.coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
            onValueChange = { value -> dragPositionMs = value },
            onValueChangeFinished = {
                dragPositionMs?.let { value -> onSeek(value.toLong()) }
                dragPositionMs = null
            },
            valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
            enabled = playback?.isReady == true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatTime(positionMs.toLong()), style = MaterialTheme.typography.labelMedium)
            Text(text = formatTime(durationMs), style = MaterialTheme.typography.labelMedium)
        }

        when {
            playback?.error != null -> Text(
                text = playback.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )

            playback?.isReady != true -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            else -> FilledIconButton(onClick = onPlayPauseClick, modifier = Modifier.size(72.dp)) {
                Icon(
                    imageVector = if (playback.isPlaying) AppIcons.Pause else AppIcons.Play,
                    contentDescription = if (playback.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

private fun technicalInfo(document: AudioDocument): String {
    return listOfNotNull(
        document.name.substringAfterLast('.', "").uppercase().takeIf { it.isNotEmpty() },
        document.sampleRateHz?.let { rate -> String.format(Locale.US, "%.1f kHz", rate / 1000.0) },
        document.channelCount?.let { channels ->
            when (channels) {
                1 -> "Mono"
                2 -> "Stereo"
                else -> "$channels channels"
            }
        },
        document.bitrateBps?.let { bitrate -> "${bitrate / 1000} kbps" }
    ).joinToString(" · ")
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

private const val ARTWORK_SIZE_PX = 720
