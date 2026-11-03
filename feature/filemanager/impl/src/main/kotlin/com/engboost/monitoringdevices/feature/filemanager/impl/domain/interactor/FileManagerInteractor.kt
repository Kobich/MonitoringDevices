package com.engboost.monitoringdevices.feature.filemanager.impl.domain.interactor

import android.net.Uri
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.CreateDirectoryResult
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.DirectoryContent
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.DirectoryNameError
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.ExportDestination
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.TransferSummary
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.repository.FileStorageRepository
import java.io.File

internal class FileManagerInteractor(
    private val repository: FileStorageRepository
) {
    /**
     * Lists [path]. If the folder no longer exists (deleted or replaced by a file),
     * falls back to the nearest existing parent.
     */
    suspend fun openDirectory(path: StoragePath): DirectoryContent {
        val existingPath = path.hierarchy()
            .asReversed()
            .firstOrNull { candidate -> repository.isDirectory(candidate) }
            ?: StoragePath.ROOT

        return DirectoryContent(
            path = existingPath,
            entries = repository.list(existingPath).sortedWith(ENTRY_ORDER)
        )
    }

    suspend fun createDirectory(parent: StoragePath, rawName: String): CreateDirectoryResult {
        val name = rawName.trim()
        val error = validateDirectoryName(name)
            ?: DirectoryNameError.ALREADY_EXISTS.takeIf { repository.exists(parent.child(name)) }

        return if (error != null) {
            CreateDirectoryResult.Invalid(error)
        } else {
            CreateDirectoryResult.Created(repository.createDirectory(parent.child(name)))
        }
    }

    suspend fun importFiles(sources: List<Uri>, destination: StoragePath): TransferSummary {
        if (sources.isEmpty()) return TransferSummary(files = 0, directories = 0)
        return repository.importFiles(sources, destination)
    }

    suspend fun importDirectory(sourceTree: Uri, destination: StoragePath): TransferSummary {
        return repository.importDirectory(sourceTree, destination)
    }

    /**
     * Export rule shared with the UI: a single file goes into a document created by the user
     * ([ExportDestination.DOCUMENT]), anything else goes into a picked folder
     * ([ExportDestination.FOLDER]).
     */
    fun exportDestination(paths: List<StoragePath>, isSingleFile: Boolean): ExportDestination {
        return if (paths.size == 1 && isSingleFile) ExportDestination.DOCUMENT else ExportDestination.FOLDER
    }

    /** [destination] must match [exportDestination]: a document URI or a tree URI. */
    suspend fun export(paths: List<StoragePath>, destination: Uri): TransferSummary {
        val singlePath = paths.singleOrNull()
        val isSingleFile = singlePath != null && !repository.isDirectory(singlePath)

        return when (exportDestination(paths, isSingleFile)) {
            ExportDestination.DOCUMENT -> {
                repository.exportFile(paths.single(), destination)
                TransferSummary(files = 1, directories = 0)
            }

            ExportDestination.FOLDER -> repository.exportToDirectory(paths, destination)
        }
    }

    /** Deletes items one by one and stops at the first failure. */
    suspend fun delete(paths: List<StoragePath>) {
        require(paths.none { path -> path.isRoot }) { "Root folder can't be deleted" }
        paths.forEach { path -> repository.delete(path) }
    }

    suspend fun fileForOpening(path: StoragePath): File {
        require(!repository.isDirectory(path)) { "$path is a folder" }
        return repository.fileOf(path)
    }

    private fun validateDirectoryName(name: String): DirectoryNameError? {
        return when {
            name.isEmpty() -> DirectoryNameError.EMPTY
            name == "." || name == ".." -> DirectoryNameError.RESERVED
            name.any { char -> char in FORBIDDEN_CHARACTERS || char.isISOControl() } ->
                DirectoryNameError.INVALID_CHARACTERS

            name.toByteArray(Charsets.UTF_8).size > MAX_NAME_BYTES -> DirectoryNameError.TOO_LONG
            else -> null
        }
    }

    private companion object {
        const val MAX_NAME_BYTES = 255
        val FORBIDDEN_CHARACTERS = setOf('/', '\\')

        val ENTRY_ORDER: Comparator<FileEntry> = compareBy<FileEntry> { entry -> !entry.isDirectory }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { entry -> entry.name }
    }
}
