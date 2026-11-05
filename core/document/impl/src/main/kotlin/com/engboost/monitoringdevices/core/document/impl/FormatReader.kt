package com.engboost.monitoringdevices.core.document.impl

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.Document
import java.io.File

internal interface FormatReader<out D : Document> {
    val formats: Set<DocumentFormat>

    suspend fun read(file: File, format: DocumentFormat, password: String?): D
}
