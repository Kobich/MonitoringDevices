package com.android.appmanager.feature.filemanager

import android.content.Context
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

/**
 * Creates the feature. One per app is enough: it holds no state of its own, the screen's state lives in a
 * ViewModel of the screen's owner. Files are kept in `filesDir/storage` of [context]'s app.
 */
fun FileManagerFeatureApi(context: Context): FileManagerFeatureApi = FileManagerFeatureImpl(context.applicationContext)
