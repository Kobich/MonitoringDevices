package com.android.appmanager.feature.filemanager.presentation.mapper

import androidx.annotation.StringRes
import com.android.appmanager.feature.filemanager.R
import com.android.appmanager.feature.filemanager.domain.model.DirectoryNameError
import com.android.appmanager.feature.filemanager.domain.model.FileEntry
import com.android.appmanager.feature.filemanager.domain.model.StoragePath
import com.android.appmanager.feature.filemanager.domain.model.TransferSummary
import com.android.appmanager.feature.filemanager.presentation.model.BreadcrumbUi
import com.android.appmanager.feature.filemanager.presentation.model.DeletionUi
import com.android.appmanager.feature.filemanager.presentation.model.FileEntryUi
import com.android.appmanager.feature.filemanager.presentation.model.PasswordDialogUi
import com.android.appmanager.feature.filemanager.presentation.model.UiText
import com.android.core.commonui.icons.FileKind
import com.android.core.fileaccess.FileAction
import javax.inject.Inject

/** Domain data to screen state. Texts stay resources: the screen resolves them. */
internal class FileManagerUiMapper @Inject constructor() {

    fun title(path: StoragePath): UiText {
        return if (path.isRoot) UiText.Resource(R.string.fm_root_title) else UiText.Plain(path.name)
    }

    fun breadcrumbs(path: StoragePath): List<BreadcrumbUi> {
        return path.hierarchy().map { item ->
            BreadcrumbUi(path = item.value, title = title(item))
        }
    }

    fun toUi(entry: FileEntry): FileEntryUi {
        return FileEntryUi(
            path = entry.path.value,
            name = entry.name,
            isDirectory = entry.isDirectory,
            kind = FileKind.of(entry.name, entry.isDirectory),
            sizeBytes = entry.sizeBytes,
            childCount = entry.childCount,
            lastModifiedMillis = entry.lastModifiedMillis
        )
    }

    fun importedText(summary: TransferSummary): UiText = UiText.Resource(R.string.fm_imported, summary.toText())

    fun exportedText(summary: TransferSummary): UiText = UiText.Resource(R.string.fm_exported, summary.toText())

    fun deletion(entries: List<FileEntryUi>): DeletionUi {
        val single = entries.singleOrNull()
        val title = if (single != null) {
            UiText.Resource(R.string.fm_delete_one_title, UiText.Plain(single.name))
        } else {
            UiText.Plural(R.plurals.fm_delete_many_title, entries.size)
        }
        val text = when {
            single != null && single.isDirectory -> R.string.fm_delete_folder_text
            single != null -> R.string.fm_delete_file_text
            entries.any { entry -> entry.isDirectory } -> R.string.fm_delete_with_folders_text
            else -> R.string.fm_delete_files_text
        }
        return DeletionUi(entries = entries, title = title, text = UiText.Resource(text))
    }

    fun deletedText(entries: List<FileEntryUi>): UiText {
        val single = entries.singleOrNull()
        return if (single != null) {
            UiText.Resource(R.string.fm_deleted_one, UiText.Plain(single.name))
        } else {
            UiText.Plural(R.plurals.fm_deleted_many, entries.size)
        }
    }

    fun passwordDialog(action: FileAction): PasswordDialogUi {
        val title = when (action) {
            FileAction.IMPORT -> R.string.fm_password_import
            FileAction.EXPORT -> R.string.fm_password_export
            FileAction.DELETE -> R.string.fm_password_delete
        }
        return PasswordDialogUi(title = title, error = null)
    }

    fun errorText(error: DirectoryNameError): UiText {
        val text = when (error) {
            DirectoryNameError.EMPTY -> R.string.fm_name_empty
            DirectoryNameError.RESERVED -> R.string.fm_name_reserved
            DirectoryNameError.INVALID_CHARACTERS -> R.string.fm_name_invalid_characters
            DirectoryNameError.TOO_LONG -> R.string.fm_name_too_long
            DirectoryNameError.ALREADY_EXISTS -> R.string.fm_name_exists
        }
        return UiText.Resource(text)
    }

    /** "Import failed: <reason>"; the reason is the error's own message and isn't translated. */
    fun failureText(@StringRes what: Int, error: Throwable): UiText {
        val reason = error.message?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName
        return UiText.Resource(R.string.fm_error_with_reason, UiText.Resource(what), UiText.Plain(reason))
    }

    fun text(@StringRes id: Int): UiText = UiText.Resource(id)

    /** "2 folders, 5 files"; "0 files" when nothing was copied. */
    private fun TransferSummary.toText(): UiText {
        val parts = buildList {
            if (directories > 0) add(UiText.Plural(R.plurals.fm_folders, directories))
            if (files > 0 || directories == 0) add(UiText.Plural(R.plurals.fm_files, files))
        }
        return UiText.Joined(parts, separator = ", ")
    }
}
