package com.android.core.document.impl

import com.android.core.document.api.DocumentFormat
import com.android.core.document.api.document.Document
import java.io.File

internal interface FormatReader<out D : Document> {
    val formats: Set<DocumentFormat>

    suspend fun read(file: File, format: DocumentFormat, password: String?): D
}
