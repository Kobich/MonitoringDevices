package com.engboost.monitoringdevices.feature.filemanager.impl.domain.model

internal data class FileEntry(
    val path: StoragePath,
    val name: String,
    val isDirectory: Boolean,
    /** Size in bytes, always 0 for folders. */
    val sizeBytes: Long,
    /** Number of direct children, always 0 for files. */
    val childCount: Int,
    val lastModifiedMillis: Long
)

internal data class DirectoryContent(
    val path: StoragePath,
    val entries: List<FileEntry>
)

internal data class TransferSummary(
    val files: Int,
    val directories: Int
)

internal enum class ExportDestination {
    /** A single document created by the user with a system "Save as" picker. */
    DOCUMENT,

    /** A folder picked by the user, items are copied inside it. */
    FOLDER
}
