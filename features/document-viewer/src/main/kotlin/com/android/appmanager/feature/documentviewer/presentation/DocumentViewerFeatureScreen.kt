package com.android.appmanager.feature.documentviewer.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.appmanager.feature.documentviewer.presentation.screen.DocumentViewerScreen
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
    val context = LocalContext.current
    val state = viewModel.uiState
    val currentOnBackClick by rememberUpdatedState(onBackClick)

    LaunchedEffect(state.isOpenInOtherAppRequested) {
        if (!state.isOpenInOtherAppRequested) return@LaunchedEffect
        viewModel.onOpenInOtherAppHandled()
        openInOtherApp(context, file)
    }

    LaunchedEffect(state.isCloseRequested) {
        if (!state.isCloseRequested) return@LaunchedEffect
        viewModel.onCloseHandled()
        currentOnBackClick()
    }

    DocumentViewerScreen(
        state = state,
        audioPlayback = viewModel.audioPlayback,
        videoPlayer = viewModel.videoPlayer,
        onBackClick = onBackClick,
        onPageSelected = viewModel::onPageSelected,
        onRetryClick = viewModel::onRetryClick,
        onOpenInOtherAppClick = viewModel::onOpenInOtherAppClick,
        onPasswordConfirm = viewModel::onPasswordConfirm,
        onPasswordDismiss = viewModel::onPasswordDismiss,
        modifier = modifier
    )
}
