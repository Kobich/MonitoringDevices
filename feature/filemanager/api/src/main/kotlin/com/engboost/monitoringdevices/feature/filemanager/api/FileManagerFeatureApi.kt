package com.engboost.monitoringdevices.feature.filemanager.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

/**
 * Viewer of the app-private storage (`Context.filesDir`).
 *
 * The screen handles in-folder navigation and selection mode itself (including system Back
 * while it is not at the root folder or while items are selected).
 *
 * @param onBackClick the user wants to leave the feature.
 * @param onFileOpen the user tapped a file; the host decides how to show it
 * (e.g. navigates to a document viewer feature).
 */
interface FileManagerFeatureApi {
    @Composable
    fun FileManagerScreen(
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    )
}
