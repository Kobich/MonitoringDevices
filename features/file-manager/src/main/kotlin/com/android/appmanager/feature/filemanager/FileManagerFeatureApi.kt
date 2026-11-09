package com.android.appmanager.feature.filemanager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

/** The screen consumes system Back while not at the root folder or while items are selected. */
interface FileManagerFeatureApi {
    @Composable
    fun FileManagerScreen(
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    )
}
