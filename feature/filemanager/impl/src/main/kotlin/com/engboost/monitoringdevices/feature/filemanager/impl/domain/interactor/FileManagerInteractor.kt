package com.engboost.monitoringdevices.feature.filemanager.impl.domain.interactor

import android.net.Uri
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.CreateDirectoryResult
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.DirectoryContent
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.DirectoryNameError
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.TransferSummary
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.repository.FileStorageRepository

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
     * [destination] must be a document URI for a file and a tree URI for a folder,
     * see [FileEntry.isDirectory].
     */
    suspend fun export(path: StoragePath, destination: Uri): TransferSummary {
        return if (repository.isDirectory(path)) {
            repository.exportDirectory(path, destination)
        } else {
            repository.exportFile(path, destination)
            TransferSummary(files = 1, directories = 0)
        }
    }

    suspend fun delete(path: StoragePath) {
        require(!path.isRoot) { "Root folder can't be deleted" }
        repository.delete(path)
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
