package com.android.appmanager.feature.filemanager.impl.presentation.mapper

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.StringRes
import com.android.appmanager.feature.filemanager.impl.R
import com.android.appmanager.feature.filemanager.impl.domain.model.DirectoryNameError
import com.android.appmanager.feature.filemanager.impl.domain.model.FileEntry
import com.android.appmanager.feature.filemanager.impl.domain.model.StoragePath
import com.android.appmanager.feature.filemanager.impl.domain.model.TransferSummary
import com.android.appmanager.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.android.appmanager.feature.filemanager.impl.presentation.model.DeletionUi
import com.android.appmanager.feature.filemanager.impl.presentation.model.FileEntryUi
import com.android.core.commonui.icons.FileKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

internal class FileManagerUiMapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resources = context.resources
    private val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    fun title(path: StoragePath): String {
        return if (path.isRoot) resources.getString(R.string.fm_root_title) else path.name
    }

    fun breadcrumbs(path: StoragePath): List<BreadcrumbUi> {
        return path.hierarchy().map { item ->
            BreadcrumbUi(path = item.value, title = title(item))
        }
    }

    fun toUi(entry: FileEntry): FileEntryUi {
        val primaryDetails = when {
            !entry.isDirectory -> Formatter.formatShortFileSize(context, entry.sizeBytes)
            entry.childCount == 0 -> resources.getString(R.string.fm_folder_empty_details)
            else -> plural(R.plurals.fm_items, entry.childCount)
        }

        return FileEntryUi(
            path = entry.path.value,
            name = entry.name,
            isDirectory = entry.isDirectory,
            kind = FileKind.of(entry.name, entry.isDirectory),
            details = "$primaryDetails · ${dateFormat.format(Date(entry.lastModifiedMillis))}"
        )
    }

    fun importedText(summary: TransferSummary): String = resources.getString(R.string.fm_imported, summary.toText())

    fun exportedText(summary: TransferSummary): String = resources.getString(R.string.fm_exported, summary.toText())

    fun deletion(entries: List<FileEntryUi>): DeletionUi {
        val single = entries.singleOrNull()
        val title = if (single != null) {
            resources.getString(R.string.fm_delete_one_title, single.name)
        } else {
            plural(R.plurals.fm_delete_many_title, entries.size)
        }
        val text = when {
            single != null && single.isDirectory -> R.string.fm_delete_folder_text
            single != null -> R.string.fm_delete_file_text
            entries.any { entry -> entry.isDirectory } -> R.string.fm_delete_with_folders_text
            else -> R.string.fm_delete_files_text
        }
        return DeletionUi(entries = entries, title = title, text = resources.getString(text))
    }

    fun deletedText(entries: List<FileEntryUi>): String {
        val single = entries.singleOrNull()
        return if (single != null) {
            resources.getString(R.string.fm_deleted_one, single.name)
        } else {
            plural(R.plurals.fm_deleted_many, entries.size)
        }
    }

    fun errorText(error: DirectoryNameError): String {
        val text = when (error) {
            DirectoryNameError.EMPTY -> R.string.fm_name_empty
            DirectoryNameError.RESERVED -> R.string.fm_name_reserved
            DirectoryNameError.INVALID_CHARACTERS -> R.string.fm_name_invalid_characters
            DirectoryNameError.TOO_LONG -> R.string.fm_name_too_long
            DirectoryNameError.ALREADY_EXISTS -> R.string.fm_name_exists
        }
        return resources.getString(text)
    }

    fun failureText(@StringRes what: Int, error: Throwable): String {
        val reason = error.message?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName
        return resources.getString(R.string.fm_error_with_reason, resources.getString(what), reason)
    }

    fun text(@StringRes id: Int): String = resources.getString(id)

    private fun TransferSummary.toText(): String {
        val parts = buildList {
            if (directories > 0) add(plural(R.plurals.fm_folders, directories))
            if (files > 0 || directories == 0) add(plural(R.plurals.fm_files, files))
        }
        return parts.joinToString(separator = ", ")
    }

    private fun plural(id: Int, count: Int): String = resources.getQuantityString(id, count, count)
}
