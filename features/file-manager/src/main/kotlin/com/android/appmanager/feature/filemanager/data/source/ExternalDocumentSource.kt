package com.android.appmanager.feature.filemanager.data.source

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

internal class ExternalDocumentSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun displayName(uri: Uri): String? {
        return DocumentFile.fromSingleUri(context, uri)?.name
    }

    fun openInput(uri: Uri): InputStream {
        return context.contentResolver.openInputStream(uri)
            ?: throw IOException("Can't read the selected document")
    }

    /**
     * Truncates the document first: with "w" some providers keep the old length, and a shorter file would end with
     * the tail of the one it replaced. "w" only for providers that don't know "wt".
     */
    fun openOutput(uri: Uri): OutputStream {
        val resolver = context.contentResolver
        val stream = try {
            resolver.openOutputStream(uri, "wt")
        } catch (error: FileNotFoundException) {
            resolver.openOutputStream(uri, "w")
        } catch (error: IllegalArgumentException) {
            resolver.openOutputStream(uri, "w")
        }
        return stream ?: throw IOException("Can't write the selected document")
    }

    /** "Save as" may hand over an existing document the user chose to replace; a new one is empty. */
    fun hasContent(uri: Uri): Boolean {
        return runCatching { (DocumentFile.fromSingleUri(context, uri)?.length() ?: 0L) > 0L }.getOrDefault(false)
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
