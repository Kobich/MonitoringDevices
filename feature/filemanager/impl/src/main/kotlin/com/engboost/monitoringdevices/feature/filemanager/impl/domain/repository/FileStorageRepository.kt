package com.engboost.monitoringdevices.feature.filemanager.impl.domain.repository

import android.net.Uri
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.TransferSummary
import java.io.File

/** All methods may throw [java.io.IOException]. */
internal interface FileStorageRepository {
    suspend fun exists(path: StoragePath): Boolean

    suspend fun isDirectory(path: StoragePath): Boolean

    suspend fun fileOf(path: StoragePath): File

    /** Unsorted. */
    suspend fun list(directory: StoragePath): List<FileEntry>

    suspend fun createDirectory(path: StoragePath): FileEntry

    suspend fun delete(path: StoragePath)

    /** Name clashes are resolved as `name (1).ext`. */
    suspend fun importFiles(sources: List<Uri>, destination: StoragePath): TransferSummary

    /** Into a new folder inside [destination]. */
    suspend fun importDirectory(sourceTree: Uri, destination: StoragePath): TransferSummary

    suspend fun exportFile(path: StoragePath, destination: Uri)

    /** Items created before a failure are removed. */
    suspend fun exportToDirectory(paths: List<StoragePath>, destinationTree: Uri): TransferSummary
}
