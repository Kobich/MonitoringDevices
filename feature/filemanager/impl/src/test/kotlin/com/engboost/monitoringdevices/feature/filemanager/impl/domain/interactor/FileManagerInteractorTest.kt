package com.engboost.monitoringdevices.feature.filemanager.impl.domain.interactor

import android.net.Uri
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.CreateDirectoryResult
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.DirectoryNameError
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.ExportDestination
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.repository.FileStorageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileManagerInteractorTest {
    private val repository = FakeFileStorageRepository()
    private val interactor = FileManagerInteractor(repository)

    @Test
    fun `lists folders first then files by name ignoring case`() = runTest {
        repository.addFile("b.txt")
        repository.addDirectory("Zeta")
        repository.addFile("A.txt")
        repository.addDirectory("alpha")

        val content = interactor.openDirectory(StoragePath.ROOT)

        assertEquals(listOf("alpha", "Zeta", "A.txt", "b.txt"), content.entries.map { it.name })
    }

    @Test
    fun `falls back to the nearest existing parent`() = runTest {
        repository.addDirectory("a")
        repository.addFile("a/file.txt")

        val content = interactor.openDirectory(StoragePath("a/deleted/deeper"))

        assertEquals(StoragePath("a"), content.path)
        assertEquals(listOf("file.txt"), content.entries.map { it.name })
    }

    @Test
    fun `validates new folder name`() = runTest {
        repository.addDirectory("existing")

        assertEquals(invalid(DirectoryNameError.EMPTY), interactor.createDirectory(StoragePath.ROOT, "  "))
        assertEquals(invalid(DirectoryNameError.RESERVED), interactor.createDirectory(StoragePath.ROOT, ".."))
        assertEquals(invalid(DirectoryNameError.INVALID_CHARACTERS), interactor.createDirectory(StoragePath.ROOT, "a/b"))
        assertEquals(invalid(DirectoryNameError.TOO_LONG), interactor.createDirectory(StoragePath.ROOT, "x".repeat(256)))
        assertEquals(invalid(DirectoryNameError.ALREADY_EXISTS), interactor.createDirectory(StoragePath.ROOT, "existing"))
    }

    @Test
    fun `creates folder with trimmed name`() = runTest {
        val result = interactor.createDirectory(StoragePath.ROOT, "  docs ")

        assertTrue(result is CreateDirectoryResult.Created)
        assertTrue(repository.isDirectory(StoragePath("docs")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `root can't be deleted`() = runTest {
        interactor.delete(listOf(StoragePath("a"), StoragePath.ROOT))
    }

    @Test
    fun `deletes all selected items`() = runTest {
        repository.addDirectory("docs")
        repository.addFile("docs/a.txt")
        repository.addFile("b.txt")
        repository.addFile("c.txt")

        interactor.delete(listOf(StoragePath("docs"), StoragePath("b.txt")))

        assertEquals(listOf("c.txt"), interactor.openDirectory(StoragePath.ROOT).entries.map { it.name })
    }

    @Test
    fun `single file is exported as a document, anything else into a folder`() {
        val file = listOf(StoragePath("a.txt"))
        val many = listOf(StoragePath("a.txt"), StoragePath("b.txt"))

        assertEquals(ExportDestination.DOCUMENT, interactor.exportDestination(file, isSingleFile = true))
        assertEquals(ExportDestination.FOLDER, interactor.exportDestination(listOf(StoragePath("dir")), isSingleFile = false))
        assertEquals(ExportDestination.FOLDER, interactor.exportDestination(many, isSingleFile = false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `folder can't be opened as a file`() = runTest {
        repository.addDirectory("docs")
        interactor.fileForOpening(StoragePath("docs"))
    }

    private fun invalid(error: DirectoryNameError) = CreateDirectoryResult.Invalid(error)
}

private class FakeFileStorageRepository : FileStorageRepository {
    private val entries = linkedMapOf<StoragePath, FileEntry>()

    fun addDirectory(path: String) = add(StoragePath(path), isDirectory = true)

    fun addFile(path: String) = add(StoragePath(path), isDirectory = false)

    private fun add(path: StoragePath, isDirectory: Boolean): FileEntry {
        val entry = FileEntry(
            path = path,
            name = path.name,
            isDirectory = isDirectory,
            sizeBytes = 0,
            childCount = 0,
            lastModifiedMillis = 0,
            mimeType = if (isDirectory) null else "text/plain"
        )
        entries[path] = entry
        return entry
    }

    override suspend fun exists(path: StoragePath) = path.isRoot || path in entries

    override suspend fun isDirectory(path: StoragePath) = path.isRoot || entries[path]?.isDirectory == true

    override suspend fun fileOf(path: StoragePath) = File("/fake", path.value)

    override suspend fun list(directory: StoragePath) = entries.values.filter { it.path.parent() == directory }

    override suspend fun createDirectory(path: StoragePath) = add(path, isDirectory = true)

    override suspend fun delete(path: StoragePath) {
        entries.keys.removeAll { it == path || it.value.startsWith(path.value + "/") }
    }

    override suspend fun importFiles(sources: List<Uri>, destination: StoragePath) = unsupported()

    override suspend fun importDirectory(sourceTree: Uri, destination: StoragePath) = unsupported()

    override suspend fun exportFile(path: StoragePath, destination: Uri) = unsupported()

    override suspend fun exportToDirectory(paths: List<StoragePath>, destinationTree: Uri) = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException()
}

