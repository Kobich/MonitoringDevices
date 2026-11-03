package com.engboost.monitoringdevices.feature.viewer.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

/** Shows a document (office files, PDF, text, images) read by `:core:document`. */
interface DocumentViewerFeatureApi {
    @Composable
    fun DocumentViewerScreen(
        file: File,
        modifier: Modifier,
        onBackClick: () -> Unit
    )
}
