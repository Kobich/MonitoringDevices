package com.android.appmanager.feature.documentviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.appmanager.feature.documentviewer.presentation.DocumentViewerFeatureScreen
import java.io.File
import javax.inject.Inject

internal class DocumentViewerFeatureImpl @Inject constructor() : DocumentViewerFeatureApi {
    @Composable
    override fun DocumentViewerScreen(file: File, modifier: Modifier, onBackClick: () -> Unit) {
        DocumentViewerFeatureScreen(file = file, onBackClick = onBackClick, modifier = modifier)
    }
}
