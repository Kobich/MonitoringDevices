package com.engboost.monitoringdevices.feature.filemanager.impl.util

import android.webkit.MimeTypeMap

internal fun mimeTypeOf(fileName: String): String {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}
