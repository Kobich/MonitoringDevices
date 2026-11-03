package com.engboost.monitoringdevices.feature.filemanager.impl.data.mapper

import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import java.io.File

internal fun File.toFileEntry(path: StoragePath): FileEntry {
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
