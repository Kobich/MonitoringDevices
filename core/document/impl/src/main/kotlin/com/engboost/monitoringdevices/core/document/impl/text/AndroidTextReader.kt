package com.engboost.monitoringdevices.core.document.impl.text

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.core.document.api.format.text.TextReader
import com.engboost.monitoringdevices.core.document.impl.source.DocumentSourceFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Reads at most [maxChars] characters of a text file with charset detection. */
internal class AndroidTextReader(
    private val sourceFiles: DocumentSourceFiles,
    private val maxChars: Int = DEFAULT_MAX_CHARS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TextReader {

    override suspend fun read(source: DocumentSource, password: String?): TextDocument {
        return readText(source, DocumentFormat.TEXT)
    }

    /** Also used for other text-based formats shown as plain text (e.g. Markdown fallback). */
    suspend fun readText(source: DocumentSource, format: DocumentFormat): TextDocument = withContext(ioDispatcher) {
        // UTF-8 needs at most 4 bytes per character; one extra byte tells whether the file is longer.
        val byteLimit = maxChars.toLong() * 4
        val bytes = try {
            sourceFiles.openInputStream(source).use { input -> input.readUpTo(byteLimit + 1) }
        } catch (error: DocumentReadException) {
            throw error
        } catch (error: IOException) {
            throw DocumentReadException(Reason.IO_ERROR, error.message, error)
        }
        val isCutByBytes = bytes.size > byteLimit
        val content = if (isCutByBytes) bytes.copyOf(byteLimit.toInt()) else bytes

        val decoded = try {
            TextDecoding.decode(content, isComplete = !isCutByBytes)
        } catch (error: TextDecoding.BinaryContentException) {
            throw DocumentReadException(Reason.UNSUPPORTED_FORMAT, error.message, error)
        }
        val isCutByChars = decoded.text.length > maxChars

        TextDocument(
            name = source.name,
            format = format,
            text = if (isCutByChars) decoded.text.take(maxChars) else decoded.text,
            charsetName = decoded.charsetName,
            isTruncated = isCutByBytes || isCutByChars
        )
    }

    private fun java.io.InputStream.readUpTo(limit: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (total < limit) {
            val read = read(buffer, 0, minOf(buffer.size.toLong(), limit - total).toInt())
            if (read < 0) break
            output.write(buffer, 0, read)
            total += read
        }
        return output.toByteArray()
    }

    companion object {
        const val DEFAULT_MAX_CHARS = 1_000_000
    }
}
