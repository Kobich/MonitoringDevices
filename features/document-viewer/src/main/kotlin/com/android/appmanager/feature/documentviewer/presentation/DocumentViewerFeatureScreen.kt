package com.android.appmanager.feature.documentviewer.presentation

import androidx.compose.runtime.Composable
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

    DocumentViewerScreen(
        state = viewModel.uiState,
        audioPlayback = viewModel.audioPlayback,
        videoPlayer = viewModel.videoPlayer,
        onBackClick = onBackClick,
        onPageSelected = viewModel::onPageSelected,
        onRetryClick = viewModel::onRetryClick,
        onOpenInOtherAppClick = {
            logClick("open_in_other_app", "icon_button", "Open in another app")
            openInOtherApp(context, file)
        },
        modifier = modifier
    )
}
