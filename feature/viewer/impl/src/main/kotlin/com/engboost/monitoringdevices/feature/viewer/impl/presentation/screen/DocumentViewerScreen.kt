package com.engboost.monitoringdevices.feature.viewer.impl.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.engboost.monitoringdevices.core.commonui.components.BackTopAppBar
import com.engboost.monitoringdevices.core.commonui.components.LoadingContent
import com.engboost.monitoringdevices.core.commonui.components.MessageContent
import com.engboost.monitoringdevices.core.commonui.components.rememberDelayedVisibility
import com.engboost.monitoringdevices.core.document.api.document.AudioDocument
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument
import com.engboost.monitoringdevices.core.document.api.document.PdfDocument
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.feature.viewer.impl.R
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.AudioPlayback
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.DocumentViewerUiState
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.ViewerContent

@Composable
internal fun DocumentViewerScreen(
    state: DocumentViewerUiState,
    audioPlayback: AudioPlayback?,
    onBackClick: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onRetryClick: () -> Unit,
    onPasswordSubmit: (String) -> Unit,
    onPasswordDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.passwordDialog?.let { dialog ->
        PasswordDialog(isWrongPassword = dialog.isWrongPassword, onSubmit = onPasswordSubmit, onDismiss = onPasswordDismiss)
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
