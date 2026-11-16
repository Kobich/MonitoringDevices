package com.android.appmanager.feature.filemanager

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.appmanager.feature.filemanager.data.repository.FileStorageRepository
import com.android.appmanager.feature.filemanager.data.source.ExternalDocumentSource
import com.android.appmanager.feature.filemanager.data.source.InternalFileSource
import com.android.appmanager.feature.filemanager.domain.interactor.FileManagerInteractor
import com.android.appmanager.feature.filemanager.presentation.FileManagerFeatureScreen
import com.android.appmanager.feature.filemanager.presentation.FileManagerViewModel
import com.android.appmanager.feature.filemanager.presentation.mapper.FileManagerUiMapper
import java.io.File

internal class FileManagerFeatureImpl(context: Context) : FileManagerFeatureApi {
    private val interactor = FileManagerInteractor(
        FileStorageRepository(
            internalSource = InternalFileSource(rootDirectory = File(context.filesDir, STORAGE_DIRECTORY)),
            documentSource = ExternalDocumentSource(context)
        )
    )
    private val uiMapper = FileManagerUiMapper(context)

    @Composable
    override fun FileManagerScreen(
        modifier: Modifier,
        onBackClick: () -> Unit,
        onFileOpen: (File) -> Unit
    ) {
        FileManagerFeatureScreen(
            onBackClick = onBackClick,
            onFileOpen = onFileOpen,
            modifier = modifier,
            viewModel = viewModel(
                factory = viewModelFactory {
                    initializer { FileManagerViewModel(interactor, uiMapper, createSavedStateHandle()) }
                }
            )
        )
    }

    companion object {
        /**
         * A folder of its own in `filesDir`: libraries keep files there too (e.g. androidx.profileinstaller's
         * `profileInstalled`), and the user shouldn't see them. Not `Context.dataDir`: it would also expose
         * databases/, shared_prefs/ and cache/. Also in `SampleFilesSeeder` of `:app`.
         */
        private const val STORAGE_DIRECTORY = "storage"
    }
}
