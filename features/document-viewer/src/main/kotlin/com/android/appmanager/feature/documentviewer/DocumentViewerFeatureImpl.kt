package com.android.appmanager.feature.documentviewer

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.appmanager.feature.documentviewer.data.player.PlayerFactory
import com.android.appmanager.feature.documentviewer.presentation.DocumentViewerFeatureScreen
import com.android.appmanager.feature.documentviewer.presentation.DocumentViewerViewModel
import com.android.core.document.api.DocumentReader
import java.io.File

internal class DocumentViewerFeatureImpl(
    context: Context,
    private val reader: DocumentReader
) : DocumentViewerFeatureApi {
    private val playerFactory = PlayerFactory(context)

    @Composable
    override fun DocumentViewerScreen(file: File, modifier: Modifier, onBackClick: () -> Unit) {
        DocumentViewerFeatureScreen(
            file = file,
            onBackClick = onBackClick,
            // A ViewModel per file: opening another file doesn't reuse the previous document.
            viewModel = viewModel(
                key = file.absolutePath,
                factory = viewModelFactory { initializer { DocumentViewerViewModel(file, reader, playerFactory) } }
            ),
            modifier = modifier
        )
    }
}
