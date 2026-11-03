package com.engboost.monitoringdevices.core.document.impl.source

import android.content.Context
import android.net.Uri
import android.graphics.ImageDecoder
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/** Gives readers a real file for any [DocumentSource]; renderers work with paths. */
internal class DocumentSourceFiles(
    private val context: Context
) {
    /** Absolute path, `file://` or `content://` URI to a source. */
    fun sourceOf(path: String): DocumentSource {
        return when {
            path.startsWith("content://") -> {
                val uri = Uri.parse(path)
                DocumentSource.Content(
                    uri = uri,
                    name = runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull()
                        ?: uri.lastPathSegment
                        ?: "document",
                    mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
                )
            }

            path.startsWith("file://") -> DocumentSource.LocalFile(File(Uri.parse(path).path.orEmpty()))
            else -> DocumentSource.LocalFile(File(path))
        }
    }

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

    /** Stream of the source content; the caller closes it. */
    fun openInputStream(source: DocumentSource): InputStream {
        return try {
            when (source) {
                is DocumentSource.LocalFile -> source.file.inputStream()
                is DocumentSource.Content -> context.contentResolver.openInputStream(source.uri)
                    ?: throw notFound(source)
            }
        } catch (error: FileNotFoundException) {
            throw DocumentReadException(DocumentReadException.Reason.NOT_FOUND, error.message, error)
        }
    }

    /** Seekable descriptor (needed by PdfRenderer); the caller closes it. */
    fun openFileDescriptor(source: DocumentSource): ParcelFileDescriptor {
        return try {
            when (source) {
                is DocumentSource.LocalFile ->
                    ParcelFileDescriptor.open(source.file, ParcelFileDescriptor.MODE_READ_ONLY)

                is DocumentSource.Content -> context.contentResolver.openFileDescriptor(source.uri, "r")
                    ?: throw notFound(source)
            }
        } catch (error: FileNotFoundException) {
            throw DocumentReadException(DocumentReadException.Reason.NOT_FOUND, error.message, error)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun imageDecoderSource(source: DocumentSource): ImageDecoder.Source {
        return when (source) {
            is DocumentSource.LocalFile -> ImageDecoder.createSource(source.file)
            is DocumentSource.Content -> ImageDecoder.createSource(context.contentResolver, source.uri)
        }
    }

    private fun notFound(source: DocumentSource): DocumentReadException {
        return DocumentReadException(DocumentReadException.Reason.NOT_FOUND, "${source.name} not found")
    }
}
