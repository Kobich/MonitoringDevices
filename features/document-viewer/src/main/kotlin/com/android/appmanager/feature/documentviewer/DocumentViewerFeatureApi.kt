package com.android.appmanager.feature.documentviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

/**
 * Follows the administrator's config from `FileAccessRules` of `:core:file-access`: a file with a message shows it
 * instead, one with a password asks for it each time the screen opens, "Open in another app" counts as export. If the
 * config takes the open file away, the screen calls `onBackClick`.
 */
interface DocumentViewerFeatureApi {
    @Composable
    fun DocumentViewerScreen(
        file: File,
        modifier: Modifier,
        onBackClick: () -> Unit
    )
}
