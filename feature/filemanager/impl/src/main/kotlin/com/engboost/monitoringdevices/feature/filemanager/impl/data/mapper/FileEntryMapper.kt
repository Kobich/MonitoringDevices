package com.engboost.monitoringdevices.feature.filemanager.impl.data.mapper

import android.webkit.MimeTypeMap
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.FileEntry
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import java.io.File

internal const val DEFAULT_MIME_TYPE = "application/octet-stream"

internal fun File.toFileEntry(path: StoragePath): FileEntry {
    val isDirectory = isDirectory
    return FileEntry(
        path = path,
        name = name,
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0L else length(),
        childCount = if (isDirectory) list()?.size ?: 0 else 0,
        lastModifiedMillis = lastModified(),
        mimeType = if (isDirectory) null else mimeTypeOf(name)
    )
}

internal fun mimeTypeOf(fileName: String): String {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    if (extension.isEmpty()) return DEFAULT_MIME_TYPE
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: DEFAULT_MIME_TYPE
}
