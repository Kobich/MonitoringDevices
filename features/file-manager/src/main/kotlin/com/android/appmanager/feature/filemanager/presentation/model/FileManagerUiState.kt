package com.android.appmanager.feature.filemanager.presentation.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.android.core.commonui.icons.FileKind
import java.io.File

@Immutable
internal data class FileManagerUiState(
    val title: UiText,
    val breadcrumbs: List<BreadcrumbUi>,
    val isRoot: Boolean,
    val entries: List<FileEntryUi>,
    val isLoading: Boolean,
    /** Import / export / delete / create is running. */
    val isBusy: Boolean,
    /** `null` in browse mode; non-null (and never empty) in selection mode. */
    val selection: SelectionUi?,
    val pendingDeletion: DeletionUi?,
    val createDirectoryDialog: CreateDirectoryDialogUi?,
    val passwordDialog: PasswordDialogUi?,
    /** The administrator's message of a file, shown instead of opening it. */
    val fileMessage: FileMessageUi?,
    /** What the config makes available. */
    val menu: MenuUi,
    /** One-shot message for a snackbar, cleared by `onMessageShown`. */
    val message: UiText?,
    /** One-shot request to show a system picker, cleared by `onPickerShown`. */
    val picker: PickerUi?,
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
    /** For files. */
    val sizeBytes: Long,
    /** For folders. */
    val childCount: Int,
    val lastModifiedMillis: Long,
    /** Opens only with a password. */
    val isLocked: Boolean = false,
    /** Shows a message instead of opening. */
    val hasMessage: Boolean = false
)

@Immutable
internal data class BreadcrumbUi(
    val path: String,
    val title: UiText
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
    val entries: List<FileEntryUi>,
    val title: UiText,
    val text: UiText
)

@Immutable
internal data class CreateDirectoryDialogUi(
    val error: UiText?
)

@Immutable
internal data class PasswordDialogUi(
    @StringRes val title: Int,
    @StringRes val error: Int?
)

/** The administrator's text and the file name: shown as they are. */
@Immutable
internal data class FileMessageUi(
    val fileName: String,
    val text: String
)

@Immutable
internal data class MenuUi(
    val canImport: Boolean,
    val canExport: Boolean,
    val canDelete: Boolean,
    /** Not in the config: available once the config has arrived, as the rest of the menu. */
    val canCreateFolder: Boolean
) {
    /** Selection is only good for export and delete. */
    val canSelect: Boolean
        get() = canExport || canDelete

    companion object {
        /** Before the config arrives. */
        val None = MenuUi(canImport = false, canExport = false, canDelete = false, canCreateFolder = false)
    }
}

@Immutable
internal sealed interface PickerUi {
    data object ImportFiles : PickerUi

    data object ImportFolder : PickerUi

    data class ExportFile(val fileName: String) : PickerUi

    data object ExportFolder : PickerUi
}
