package com.engboost.monitoringdevices.feature.viewer.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.screen.DocumentViewerScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

@Composable
internal fun DocumentViewerFeatureScreen(
    file: File,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentViewerViewModel = koinViewModel(
        key = file.absolutePath,
        parameters = { parametersOf(file) }
    )
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onScreenStopped()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DocumentViewerScreen(
        state = viewModel.uiState,
        onBackClick = onBackClick,
        onChartRenderingSelected = viewModel::onChartRenderingSelected,
        onPageSelected = viewModel::onPageSelected,
        onPlayPauseClick = viewModel::onPlayPauseClick,
        onSeek = viewModel::onSeek,
        onRetryClick = viewModel::onRetryClick,
        onPasswordSubmit = viewModel::onPasswordSubmit,
        onPasswordDismiss = viewModel::onPasswordDismiss,
        modifier = modifier
    )
}
