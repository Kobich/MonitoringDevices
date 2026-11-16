package com.android.appmanager.feature.filemanager.data.repository

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.android.appmanager.feature.filemanager.data.source.ExternalDocumentSource
import com.android.appmanager.feature.filemanager.data.source.InternalFileSource
import com.android.appmanager.feature.filemanager.domain.model.FileEntry
import com.android.appmanager.feature.filemanager.domain.model.StoragePath
import com.android.appmanager.feature.filemanager.domain.model.TransferSummary
import com.android.appmanager.feature.filemanager.util.mimeTypeOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

private const val TAG = "FileStorage"

/** All methods may throw [IOException]. */
internal class FileStorageRepository @Inject constructor(
    private val internalSource: InternalFileSource,
    private val documentSource: ExternalDocumentSource
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun exists(path: StoragePath): Boolean = io {
        internalSource.resolve(path).exists()
    }

    suspend fun isDirectory(path: StoragePath): Boolean = io {
        internalSource.resolve(path).isDirectory
    }

    suspend fun fileOf(path: StoragePath): File = io {
        internalSource.resolve(path).also { file ->
            if (!file.isFile) throw IOException("$path is not a file")
        }
    }

    /** Unsorted. */
    suspend fun list(directory: StoragePath): List<FileEntry> = io {
        val children = internalSource.resolve(directory).listFiles()
            ?: throw IOException("Can't read folder $directory")

        children.map { file -> file.toFileEntry(directory.child(file.name)) }
    }

    suspend fun createDirectory(path: StoragePath): Unit = io {
        if (!internalSource.resolve(path).mkdirs()) throw IOException("Can't create folder $path")
    }

    suspend fun delete(path: StoragePath): Unit = io {
        val file = internalSource.resolve(path)
        if (file == internalSource.resolve(StoragePath.ROOT)) {
            throw IOException("Root folder can't be deleted")
        }
        if (!file.deleteRecursively()) {
            throw IOException("Can't delete $path")
        }
    }

    /** Name clashes are resolved as `name (1).ext`. */
    suspend fun importFiles(
        sources: List<Uri>,
        destination: StoragePath
    ): TransferSummary = io {
        val directory = internalSource.resolve(destination)
        sources.forEach { uri ->
            coroutineContext.ensureActive()
            val name = documentSource.displayName(uri) ?: uri.lastPathSegment ?: "file"
            copyDocumentToFile(uri, internalSource.uniqueChild(directory, name))
        }
        Log.d(TAG, "importFiles: ${sources.size} file(s) into $destination")
        TransferSummary(files = sources.size, directories = 0)
    }

    /** Into a new folder inside [destination]. */
    suspend fun importDirectory(
        sourceTree: Uri,
        destination: StoragePath
    ): TransferSummary = io {
        val tree = documentSource.tree(sourceTree)
        val target = internalSource.uniqueChild(
            directory = internalSource.resolve(destination),
            rawName = tree.name ?: "folder"
        )
        val counter = TransferCounter()

        try {
            target.mkdirOrThrow()
            counter.directories++
            copyDocumentTree(tree, target, counter)
        } catch (error: Throwable) {
            Log.w(TAG, "importDirectory: failed, removing partial copy ${target.name}", error)
            target.deleteRecursively()
            throw error
        }
        counter.toSummary()
    }

    suspend fun exportFile(path: StoragePath, destination: Uri): Unit = io {
        try {
            internalSource.resolve(path).inputStream().use { input ->
                documentSource.openOutput(destination).use { output -> input.copyTo(output) }
            }
        } catch (error: Throwable) {
            Log.w(TAG, "exportFile: $path failed, removing the created document", error)
            documentSource.deleteSingle(destination)
            throw error
        }
    }

    /** Items created before a failure are removed. */
    suspend fun exportToDirectory(
        paths: List<StoragePath>,
        destinationTree: Uri
    ): TransferSummary = io {
        val tree = documentSource.tree(destinationTree)
        val counter = TransferCounter()
        val createdItems = mutableListOf<DocumentFile>()

        try {
            paths.forEach { path ->
                coroutineContext.ensureActive()
                exportItem(internalSource.resolve(path), tree, counter) { item -> createdItems += item }
            }
        } catch (error: Throwable) {
            Log.w(TAG, "exportToDirectory: failed, removing ${createdItems.size} created item(s)", error)
            createdItems.forEach { item -> runCatching { item.delete() } }
            throw error
        }
        counter.toSummary()
    }

    private suspend fun copyDocumentTree(
        source: DocumentFile,
        target: File,
        counter: TransferCounter
    ) {
        source.listFiles().forEach { child ->
            coroutineContext.ensureActive()
            val childTarget = internalSource.uniqueChild(target, child.name ?: "unnamed")
            when {
                child.isDirectory -> {
                    childTarget.mkdirOrThrow()
                    counter.directories++
                    copyDocumentTree(child, childTarget, counter)
                }

                child.isFile -> {
                    copyDocumentToFile(child.uri, childTarget)
                    counter.files++
                }
            }
        }
    }

    /** [onCreated] receives the created top-level item. */
    private suspend fun exportItem(
        source: File,
        target: DocumentFile,
        counter: TransferCounter,
        onCreated: (DocumentFile) -> Unit = {}
    ) {
        if (source.isDirectory) {
            val directory = target.createDirectoryOrThrow(source.name)
            onCreated(directory)
            counter.directories++

            val children = source.listFiles() ?: throw IOException("Can't read folder ${source.name}")
            children.forEach { child ->
                coroutineContext.ensureActive()
                exportItem(child, directory, counter)
            }
        } else {
            val document = target.createFile(mimeTypeOf(source.name), source.name)
                ?: throw IOException("Can't create ${source.name} in the selected folder")
            onCreated(document)
            source.inputStream().use { input ->
                documentSource.openOutput(document.uri).use { output -> input.copyTo(output) }
            }
            counter.files++
        }
    }

    private fun copyDocumentToFile(source: Uri, target: File) {
        try {
            documentSource.openInput(source).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(ioDispatcher) { block() }

    private class TransferCounter(
        var files: Int = 0,
        var directories: Int = 0
    ) {
        fun toSummary() = TransferSummary(files = files, directories = directories)
    }
}

private fun File.toFileEntry(path: StoragePath): FileEntry {
    val isDirectory = isDirectory
    return FileEntry(
        path = path,
        name = name,
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0L else length(),
        childCount = if (isDirectory) list()?.size ?: 0 else 0,
        lastModifiedMillis = lastModified()
    )
}

private fun File.mkdirOrThrow() {
    if (!mkdir()) throw IOException("Can't create folder $name")
}

private fun DocumentFile.createDirectoryOrThrow(name: String): DocumentFile {
    return createDirectory(name) ?: throw IOException("Can't create folder $name in the selected location")
}
