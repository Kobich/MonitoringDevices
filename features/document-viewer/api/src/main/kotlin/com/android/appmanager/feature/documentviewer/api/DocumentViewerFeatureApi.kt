package com.android.appmanager.feature.documentviewer.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

interface DocumentViewerFeatureApi {
    @Composable
    fun DocumentViewerScreen(
        file: File,
        modifier: Modifier,
        onBackClick: () -> Unit
    )
}
