package com.engboost.monitoringdevices.feature.filemanager.impl.util

import android.webkit.MimeTypeMap

/** Android's document pickers want a MIME type for a new file; derived from the name. */
internal fun mimeTypeOf(fileName: String): String {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}
