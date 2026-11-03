package com.engboost.monitoringdevices.core.document.api

import android.net.Uri
import java.io.File

/** Where a document is read from. Readers accept both kinds. */
sealed interface DocumentSource {
    /** Display name used for type detection and titles. */
    val name: String

    /** A file the app can access directly, e.g. inside `Context.filesDir`. */
    data class LocalFile(val file: File) : DocumentSource {
        override val name: String
            get() = file.name
    }

    /** A `content://` document, e.g. picked through the Storage Access Framework. */
    data class Content(
        val uri: Uri,
        override val name: String,
        val mimeType: String? = null
    ) : DocumentSource
}
