package com.android.appmanager.feature.filemanager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

/** The screen consumes system Back while not at the root folder or while items are selected. */
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

    /**
     * Shows [storage] with [passwords]: their files are opened, exported and deleted only after the password.
     * `null` is the same as no passwords. New passwords apply right away and forget the ones already entered.
     */
    @Composable
    fun FileManagerScreen(
        storage: FileStorage,
        passwords: FilePasswords?,
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    )
}
