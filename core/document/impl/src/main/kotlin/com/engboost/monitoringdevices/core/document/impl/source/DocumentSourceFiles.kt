package com.engboost.monitoringdevices.core.document.impl.source

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import java.io.File
import java.io.IOException

/** Gives readers a real file for any [DocumentSource]; renderers work with paths. */
internal class DocumentSourceFiles(
    private val context: Context
) {
    /** Stable key of the source content, used for caching. */
    fun identity(source: DocumentSource): String {
        return when (source) {
            is DocumentSource.LocalFile -> {
                val file = source.file
                if (!file.isFile) throw notFound(source)
                "${file.canonicalPath}|${file.length()}|${file.lastModified()}"
            }

            is DocumentSource.Content -> {
                val document = DocumentFile.fromSingleUri(context, source.uri)
                "${source.uri}|${document?.length()}|${document?.lastModified()}"
            }
        }
    }

    /** Runs [block] with a file for [source]; content URIs are copied into [workDirectory]. */
    suspend fun <T> withFile(source: DocumentSource, workDirectory: File, block: suspend (File) -> T): T {
        return when (source) {
            is DocumentSource.LocalFile -> {
                if (!source.file.isFile) throw notFound(source)
                block(source.file)
            }

            is DocumentSource.Content -> {
                val copy = File(workDirectory, "source-${source.name.replace('/', '_')}")
                try {
                    val input = context.contentResolver.openInputStream(source.uri) ?: throw notFound(source)
                    input.use { stream -> copy.outputStream().use { output -> stream.copyTo(output) } }
                } catch (error: IOException) {
                    throw DocumentReadException(DocumentReadException.Reason.IO_ERROR, error.message, error)
                }
                block(copy)
            }
        }
    }

    private fun notFound(source: DocumentSource): DocumentReadException {
        return DocumentReadException(DocumentReadException.Reason.NOT_FOUND, "${source.name} not found")
    }
}
