package com.engboost.monitoringdevices.feature.filemanager.impl.domain.repository

import android.net.Uri
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.TransferSummary
import java.io.File

/**
 * Access to the app-private storage plus transfers between it and external documents
 * (Storage Access Framework URIs). All methods may throw [java.io.IOException].
 */
internal interface FileStorageRepository {
    suspend fun exists(path: StoragePath): Boolean

    suspend fun isDirectory(path: StoragePath): Boolean

    /** Real file behind [path], e.g. to hand it over to a viewer. */
    suspend fun fileOf(path: StoragePath): File

    /** Direct children of [directory], unsorted. */
    suspend fun list(directory: StoragePath): List<FileEntry>

    suspend fun createDirectory(path: StoragePath): FileEntry

    /** Deletes a file or a folder with all its content. */
    suspend fun delete(path: StoragePath)

    /** Copies documents into [destination]. Name clashes are resolved as `name (1).ext`. */
    suspend fun importFiles(sources: List<Uri>, destination: StoragePath): TransferSummary

    /** Recursively copies a document tree into a new folder inside [destination]. */
    suspend fun importDirectory(sourceTree: Uri, destination: StoragePath): TransferSummary

    /** Writes the file content into an already created document. */
    suspend fun exportFile(path: StoragePath, destination: Uri)

    /**
     * Copies files and folders (recursively) into [destinationTree].
     * Items created before a failure are removed.
     */
    suspend fun exportToDirectory(paths: List<StoragePath>, destinationTree: Uri): TransferSummary
}
