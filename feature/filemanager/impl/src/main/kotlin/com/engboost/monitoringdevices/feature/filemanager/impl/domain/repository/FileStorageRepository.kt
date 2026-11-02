package com.engboost.monitoringdevices.feature.filemanager.impl.domain.repository

import android.net.Uri
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.TransferSummary

/**
 * Access to the app-private storage plus transfers between it and external documents
 * (Storage Access Framework URIs). All methods may throw [java.io.IOException].
 */
internal interface FileStorageRepository {
    suspend fun exists(path: StoragePath): Boolean

    suspend fun isDirectory(path: StoragePath): Boolean

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

    /** Recursively copies a folder into a new folder inside [destinationTree]. */
    suspend fun exportDirectory(path: StoragePath, destinationTree: Uri): TransferSummary
}
