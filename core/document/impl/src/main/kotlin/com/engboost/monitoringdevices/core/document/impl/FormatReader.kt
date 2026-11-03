package com.engboost.monitoringdevices.core.document.impl

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.Document
import java.io.File

/** Reads the [formats] it declares; [CompositeDocumentReader] picks one by file extension. */
internal interface FormatReader<out D : Document> {
    val formats: Set<DocumentFormat>

    /** @param format one of [formats], already detected from the file extension. */
    suspend fun read(file: File, format: DocumentFormat, password: String?): D
}
