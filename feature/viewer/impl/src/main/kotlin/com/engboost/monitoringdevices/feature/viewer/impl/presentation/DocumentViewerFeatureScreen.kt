package com.engboost.monitoringdevices.feature.viewer.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.screen.DocumentViewerScreen
import java.io.File

@Composable
internal fun DocumentViewerFeatureScreen(
    file: File,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentViewerViewModel = hiltViewModel<DocumentViewerViewModel, DocumentViewerViewModel.Factory>(
        key = file.absolutePath,
        creationCallback = { factory -> factory.create(file) }
    )
) {
    // Don't keep playing audio behind the user's back.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onScreenStopped() }

    DocumentViewerScreen(
        state = viewModel.uiState,
        onBackClick = onBackClick,
        onPageSelected = viewModel::onPageSelected,
        onPlayPauseClick = viewModel::onPlayPauseClick,
        onSeek = viewModel::onSeek,
        onRetryClick = viewModel::onRetryClick,
        onPasswordSubmit = viewModel::onPasswordSubmit,
        onPasswordDismiss = viewModel::onPasswordDismiss,
        modifier = modifier
    )
}
