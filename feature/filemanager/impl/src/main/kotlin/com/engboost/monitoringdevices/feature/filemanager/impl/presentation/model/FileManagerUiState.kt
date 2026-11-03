package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model

import androidx.compose.runtime.Immutable
import com.engboost.monitoringdevices.core.commonui.icons.FileKind
import java.io.File

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
    /** `null` in browse mode; non-null (and never empty) in selection mode. */
    val selection: SelectionUi?,
    val pendingDeletion: DeletionUi?,
    val createDirectoryDialog: CreateDirectoryDialogUi?,
    /** One-shot message for a snackbar, cleared by `onMessageShown`. */
    val message: String?,
    /** One-shot request to show a system picker, cleared by `onExportPickerShown`. */
    val exportPicker: ExportPickerUi?,
    /** One-shot request to open a file, cleared by `onFileOpenHandled`. */
    val fileToOpen: File?
) {
    val isSelectionMode: Boolean
        get() = selection != null
}

@Immutable
internal data class FileEntryUi(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val kind: FileKind,
    val details: String,
    val mimeType: String
)

@Immutable
internal data class BreadcrumbUi(
    val path: String,
    val title: String
)

@Immutable
internal data class SelectionUi(
    val selectedPaths: Set<String>
) {
    val count: Int
        get() = selectedPaths.size

    operator fun contains(entry: FileEntryUi): Boolean = entry.path in selectedPaths
}

@Immutable
internal data class DeletionUi(
    val title: String,
    val text: String
)

@Immutable
internal data class CreateDirectoryDialogUi(
    val error: String?
)

@Immutable
internal sealed interface ExportPickerUi {
    /** "Save as" picker for a single file. */
    data class CreateDocument(val fileName: String, val mimeType: String) : ExportPickerUi

    /** Folder picker, selected items are copied inside. */
    data object PickFolder : ExportPickerUi
}
