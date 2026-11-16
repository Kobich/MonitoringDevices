package com.android.appmanager.feature.filemanager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

/**
 * The screen consumes system Back while not at the root folder or while items are selected. What it allows follows the
 * administrator's config from `FileAccessRules` of `:core:file-access`.
 */
interface FileManagerFeatureApi {
    /** Shows [FileStorage.Default]. */
    @Composable
    fun FileManagerScreen(
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    )

    /** Shows [storage], starting at its root; the user can't go above it. */
    @Composable
    fun FileManagerScreen(
        storage: FileStorage,
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    )
}
