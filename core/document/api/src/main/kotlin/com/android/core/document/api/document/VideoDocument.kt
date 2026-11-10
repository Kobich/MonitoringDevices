package com.android.core.document.api.document

import com.android.core.document.api.DocumentFormat
import java.io.File

data class VideoDocument(
    override val name: String,
    override val format: DocumentFormat,
    val file: File
) : Document
