package com.engboost.monitoringdevices.feature.filemanager.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Viewer of the app-private storage (`Context.filesDir`).
 *
 * The screen handles in-folder navigation itself (including system Back while it is not
 * at the root folder). [onBackClick] is called when the user wants to leave the feature.
 */
interface FileManagerFeatureApi {
    @Composable
    fun FileManagerScreen(
        modifier: Modifier,
        onBackClick: () -> Unit
    )
}
