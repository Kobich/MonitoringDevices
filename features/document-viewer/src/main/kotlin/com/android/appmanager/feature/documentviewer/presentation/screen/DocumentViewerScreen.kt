package com.android.appmanager.feature.documentviewer.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import com.android.appmanager.feature.documentviewer.R
import com.android.appmanager.feature.documentviewer.presentation.AudioPlayback
import com.android.appmanager.feature.documentviewer.presentation.model.DocumentViewerUiState
import com.android.appmanager.feature.documentviewer.presentation.model.ViewerContent
import com.android.core.commonui.components.BackTopAppBar
import com.android.core.commonui.components.LoadingContent
import com.android.core.commonui.components.MessageContent
import com.android.core.commonui.state.rememberDelayedVisibility
import com.android.core.document.api.document.AudioDocument
import com.android.core.document.api.document.HtmlDocument
import com.android.core.document.api.document.ImageDocument
import com.android.core.document.api.document.PdfDocument
import com.android.core.document.api.document.TextDocument
import com.android.core.document.api.document.VideoDocument

@Composable
internal fun DocumentViewerScreen(
    state: DocumentViewerUiState,
    audioPlayback: AudioPlayback?,
    videoPlayer: Player?,
    onBackClick: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopAppBar(title = state.title, onNavigationClick = onBackClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val isLoadingVisible = rememberDelayedVisibility(state.content is ViewerContent.Loading)
            when (val content = state.content) {
                ViewerContent.Loading -> if (isLoadingVisible) LoadingContent(text = stringResource(R.string.viewer_opening))

                is ViewerContent.Loaded -> {
                    val contentModifier = Modifier.fillMaxSize()
                    when (val document = content.document) {
                        is HtmlDocument -> HtmlDocumentContent(
                            document = document,
                            selectedPageIndex = content.selectedPageIndex,
                            onPageSelected = onPageSelected,
                            modifier = contentModifier
                        )

                        is PdfDocument -> PdfDocumentContent(document = document, modifier = contentModifier)
                        is TextDocument -> TextDocumentContent(document = document, modifier = contentModifier)
                        is ImageDocument -> ImageDocumentContent(document = document, modifier = contentModifier)
                        is AudioDocument -> AudioDocumentContent(
                            document = document,
                            playback = audioPlayback,
                            modifier = contentModifier
                        )

                        is VideoDocument -> VideoDocumentContent(player = videoPlayer, modifier = contentModifier)
                    }
                }

                is ViewerContent.Error -> MessageContent(
                    title = stringResource(content.title),
                    text = content.details,
                    actionLabel = stringResource(R.string.viewer_retry),
                    onActionClick = onRetryClick
                )
            }
        }
    }
}
