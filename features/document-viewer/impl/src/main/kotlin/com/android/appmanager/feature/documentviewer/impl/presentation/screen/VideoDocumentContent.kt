package com.android.appmanager.feature.documentviewer.impl.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

@Composable
internal fun VideoDocumentContent(
    player: Player?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context -> PlayerView(context) },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier.background(Color.Black)
    )
}
