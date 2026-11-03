package com.engboost.monitoringdevices.feature.viewer.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    DocumentViewerScreen(
        state = viewModel.uiState,
        onBackClick = onBackClick,
        onChartRenderingSelected = viewModel::onChartRenderingSelected,
        onPageSelected = viewModel::onPageSelected,
        onRetryClick = viewModel::onRetryClick,
        onPasswordSubmit = viewModel::onPasswordSubmit,
        onPasswordDismiss = viewModel::onPasswordDismiss,
        modifier = modifier
    )
}
