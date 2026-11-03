package com.engboost.monitoringdevices.feature.viewer.impl.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.core.commonui.components.BackTopAppBar
import com.engboost.monitoringdevices.core.commonui.components.LoadingContent
import com.engboost.monitoringdevices.core.commonui.components.MessageContent
import com.engboost.monitoringdevices.core.commonui.components.rememberDelayedVisibility
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.DocumentViewerUiState
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.ViewerContent

@Composable
internal fun DocumentViewerScreen(
    state: DocumentViewerUiState,
    onBackClick: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onPasswordSubmit: (String) -> Unit,
    onPasswordDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.passwordDialog?.let { dialog ->
        PasswordDialog(error = dialog.error, onSubmit = onPasswordSubmit, onDismiss = onPasswordDismiss)
    }

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
                ViewerContent.Loading -> if (isLoadingVisible) LoadingContent(text = "Opening…")

                is ViewerContent.Html -> HtmlDocumentContent(
                    document = content.document,
                    selectedPageIndex = content.selectedPageIndex,
                    onPageSelected = onPageSelected,
                    modifier = Modifier.fillMaxSize()
                )

                is ViewerContent.Pdf -> PdfDocumentContent(document = content.document, modifier = Modifier.fillMaxSize())
                is ViewerContent.Text -> TextDocumentContent(document = content.document, modifier = Modifier.fillMaxSize())
                is ViewerContent.Image -> ImageDocumentContent(document = content.document, modifier = Modifier.fillMaxSize())
                is ViewerContent.Audio -> AudioDocumentContent(
                    document = content.document,
                    playback = state.audioPlayback,
                    onPlayPauseClick = onPlayPauseClick,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxSize()
                )

                is ViewerContent.Error -> MessageContent(
                    title = content.message,
                    actionLabel = "Retry",
                    onActionClick = onRetryClick
                )
            }
        }
    }
}
