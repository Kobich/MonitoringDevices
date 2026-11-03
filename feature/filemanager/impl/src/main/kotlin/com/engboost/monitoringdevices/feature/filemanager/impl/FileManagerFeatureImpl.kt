package com.engboost.monitoringdevices.feature.filemanager.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.feature.filemanager.api.FileManagerFeatureApi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.FileManagerFeatureScreen
import java.io.File
import javax.inject.Inject

internal class FileManagerFeatureImpl @Inject constructor() : FileManagerFeatureApi {
    @Composable
    override fun FileManagerScreen(
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    ) {
        FileManagerFeatureScreen(
            onBackClick = onBackClick,
            onFileOpen = onFileOpen,
            modifier = modifier
        )
    }
}
