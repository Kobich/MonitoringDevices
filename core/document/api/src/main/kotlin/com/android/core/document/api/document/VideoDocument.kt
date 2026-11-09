package com.android.core.document.api.document

import com.android.core.document.api.DocumentFormat
import java.io.File

/** Playback is up to the viewer, which plays [file]. */
data class VideoDocument(
    override val name: String,
    override val format: DocumentFormat,
    val file: File
) : Document
