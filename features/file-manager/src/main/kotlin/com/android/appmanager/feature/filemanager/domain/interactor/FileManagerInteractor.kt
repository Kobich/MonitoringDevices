package com.android.appmanager.feature.filemanager.domain.interactor

import android.net.Uri
import com.android.appmanager.feature.filemanager.data.repository.FileStorageRepository
import com.android.appmanager.feature.filemanager.domain.model.DirectoryContent
import com.android.appmanager.feature.filemanager.domain.model.DirectoryNameError
import com.android.appmanager.feature.filemanager.domain.model.FileEntry
import com.android.appmanager.feature.filemanager.domain.model.StoragePath
import com.android.appmanager.feature.filemanager.domain.model.TransferSummary
import java.io.File
import javax.inject.Inject

internal class FileManagerInteractor @Inject constructor(
    private val repository: FileStorageRepository
) {
    /** Falls back to the nearest existing parent if the folder no longer exists. */
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

    /** `null` when the folder is created. */
    suspend fun createDirectory(parent: StoragePath, rawName: String): DirectoryNameError? {
        val name = rawName.trim()
        val error = validateDirectoryName(name)
            ?: DirectoryNameError.ALREADY_EXISTS.takeIf { repository.exists(parent.child(name)) }

        if (error == null) repository.createDirectory(parent.child(name))
        return error
    }

    suspend fun importFiles(sources: List<Uri>, destination: StoragePath): TransferSummary =
        repository.importFiles(sources, destination)

    suspend fun importDirectory(sourceTree: Uri, destination: StoragePath): TransferSummary =
        repository.importDirectory(sourceTree, destination)

    suspend fun exportFile(path: StoragePath, document: Uri): TransferSummary {
        repository.exportFile(path, document)
        return TransferSummary(files = 1, directories = 0)
    }

    suspend fun exportToFolder(paths: List<StoragePath>, folder: Uri): TransferSummary =
        repository.exportToDirectory(paths, folder)

    /** Stops at the first failure. */
    suspend fun delete(paths: List<StoragePath>) {
        paths.forEach { path -> repository.delete(path) }
    }

    suspend fun fileOf(path: StoragePath): File = repository.fileOf(path)

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
