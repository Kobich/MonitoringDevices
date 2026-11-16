package com.android.appmanager.feature.filemanager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.appmanager.feature.filemanager.presentation.FileManagerFeatureScreen
import java.io.File
import javax.inject.Inject

internal class FileManagerFeatureImpl @Inject constructor() : FileManagerFeatureApi {
    @Composable
    override fun FileManagerScreen(
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    ) {
        FileManagerScreen(
            storage = FileStorage.Default,
            modifier = modifier,
            onBackClick = onBackClick,
            onFileOpen = onFileOpen
        )
    }

    @Composable
    override fun FileManagerScreen(
        storage: FileStorage,
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    ) {
        FileManagerScreen(
            storage = storage,
            passwords = null,
            modifier = modifier,
            onBackClick = onBackClick,
            onFileOpen = onFileOpen
        )
    }

    @Composable
    override fun FileManagerScreen(
        storage: FileStorage,
        passwords: FilePasswords?,
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    ) {
        FileManagerFeatureScreen(
            storage = storage,
            passwords = passwords,
            onBackClick = onBackClick,
            onFileOpen = onFileOpen,
            modifier = modifier
        )
    }
}
