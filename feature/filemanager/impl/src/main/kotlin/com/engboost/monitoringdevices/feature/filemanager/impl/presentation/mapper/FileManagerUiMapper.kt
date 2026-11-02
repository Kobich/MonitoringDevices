package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.mapper

import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.DirectoryNameError
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.TransferSummary
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi
import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal class FileManagerUiMapper {
    private val dateFormat: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())

    fun title(path: StoragePath): String {
        return if (path.isRoot) ROOT_TITLE else path.name
    }

    fun breadcrumbs(path: StoragePath): List<BreadcrumbUi> {
        return path.hierarchy().map { item ->
            BreadcrumbUi(path = item.value, title = title(item))
        }
    }

    fun toUi(entry: FileEntry): FileEntryUi {
        val primaryDetails = if (entry.isDirectory) {
            entry.childCount.toItemsText()
        } else {
            entry.sizeBytes.toReadableSize()
        }

        return FileEntryUi(
            path = entry.path.value,
            name = entry.name,
            isDirectory = entry.isDirectory,
            details = "$primaryDetails · ${dateFormat.format(Date(entry.lastModifiedMillis))}",
            mimeType = entry.mimeType ?: DIRECTORY_MIME_TYPE
        )
    }

    fun importedText(summary: TransferSummary): String = "Imported ${summary.toText()}"

    fun exportedText(name: String, summary: TransferSummary): String {
        return if (summary.directories == 0) "Exported \"$name\"" else "Exported ${summary.toText()}"
    }

    fun errorText(error: DirectoryNameError): String {
        return when (error) {
            DirectoryNameError.EMPTY -> "Enter a name"
            DirectoryNameError.RESERVED -> "This name is reserved"
            DirectoryNameError.INVALID_CHARACTERS -> "Name can't contain / or \\"
            DirectoryNameError.TOO_LONG -> "Name is too long"
            DirectoryNameError.ALREADY_EXISTS -> "A file or folder with this name already exists"
        }
    }

    private fun TransferSummary.toText(): String {
        val parts = buildList {
            if (directories > 0) add(directories.plural("folder", "folders"))
            if (files > 0 || directories == 0) add(files.plural("file", "files"))
        }
        return parts.joinToString(separator = ", ")
    }

    private fun Int.toItemsText(): String = if (this == 0) "Empty" else plural("item", "items")

    private fun Int.plural(one: String, many: String): String = "$this ${if (this == 1) one else many}"

    private fun Long.toReadableSize(): String {
        if (this < BYTES_IN_KB) return "$this B"

        var value = toDouble() / BYTES_IN_KB
        var unitIndex = 0
        while (value >= BYTES_IN_KB && unitIndex < SIZE_UNITS.lastIndex) {
            value /= BYTES_IN_KB
            unitIndex++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, SIZE_UNITS[unitIndex])
    }

    private companion object {
        const val ROOT_TITLE = "App files"
        const val DIRECTORY_MIME_TYPE = "inode/directory"
        const val BYTES_IN_KB = 1024
        val SIZE_UNITS = listOf("KB", "MB", "GB", "TB")
    }
}
