package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class FileManagerUiState(
    val title: String,
    val breadcrumbs: List<BreadcrumbUi>,
    val isRoot: Boolean,
    val entries: List<FileEntryUi>,
    /** Folder content is being read. */
    val isLoading: Boolean,
    /** Import / export / delete / create is running, other operations are disabled. */
    val isBusy: Boolean,
    /** One-shot message for a snackbar, cleared by `onMessageShown`. */
    val message: String?,
    val pendingDeletion: FileEntryUi?,
    val createDirectoryDialog: CreateDirectoryDialogUi?
)

@Immutable
internal data class FileEntryUi(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val details: String,
    val mimeType: String
)

@Immutable
internal data class BreadcrumbUi(
    val path: String,
    val title: String
)

@Immutable
internal data class CreateDirectoryDialogUi(
    val error: String?
)
