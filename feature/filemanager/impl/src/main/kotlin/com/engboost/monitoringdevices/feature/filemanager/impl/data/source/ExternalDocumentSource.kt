package com.engboost.monitoringdevices.feature.filemanager.impl.data.source

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Storage Access Framework documents picked by the user. */
internal class ExternalDocumentSource(
    private val context: Context
) {
    fun displayName(uri: Uri): String? {
        return DocumentFile.fromSingleUri(context, uri)?.name
    }

    fun openInput(uri: Uri): InputStream {
        return context.contentResolver.openInputStream(uri)
            ?: throw IOException("Can't read the selected document")
    }

    fun openOutput(uri: Uri): OutputStream {
        return context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("Can't write the selected document")
    }

    fun tree(treeUri: Uri): DocumentFile {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
        if (tree == null || !tree.isDirectory) {
            throw IOException("Can't access the selected folder")
        }
        return tree
    }

    fun deleteSingle(uri: Uri) {
        runCatching { DocumentFile.fromSingleUri(context, uri)?.delete() }
    }
}
