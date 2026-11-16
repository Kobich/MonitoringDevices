package com.android.appmanager.feature.documentviewer

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.core.document.api.DocumentReader
import java.io.File

interface DocumentViewerFeatureApi {
    @Composable
    fun DocumentViewerScreen(
        file: File,
        modifier: Modifier,
        onBackClick: () -> Unit
    )
}

/**
 * Creates the feature. One per app is enough. [reader] opens the files, e.g. `DocumentCore(context).reader`
 * from `:core:document:impl`, itself one per app.
 */
fun DocumentViewerFeatureApi(context: Context, reader: DocumentReader): DocumentViewerFeatureApi =
    DocumentViewerFeatureImpl(context.applicationContext, reader)
