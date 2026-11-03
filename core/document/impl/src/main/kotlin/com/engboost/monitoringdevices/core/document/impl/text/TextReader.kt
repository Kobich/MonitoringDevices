package com.engboost.monitoringdevices.core.document.impl.text

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.core.document.impl.FormatReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * Reads at most [maxChars] characters of a text file with charset detection.
 * Also shows Markdown as plain text when it can't be formatted, see [MarkdownReader].
 */
internal class TextReader(
    private val maxChars: Int = DEFAULT_MAX_CHARS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FormatReader<TextDocument> {
    override val formats = setOf(DocumentFormat.TXT, DocumentFormat.LOG, DocumentFormat.JSON, DocumentFormat.XML)

    override suspend fun read(file: File, format: DocumentFormat, password: String?): TextDocument = withContext(ioDispatcher) {
        // UTF-8 needs at most 4 bytes per character; one extra byte tells whether the file is longer.
        val byteLimit = maxChars.toLong() * 4
        val bytes = file.inputStream().use { input -> input.readUpTo(byteLimit + 1) }
        val isCutByBytes = bytes.size > byteLimit
        val content = if (isCutByBytes) bytes.copyOf(byteLimit.toInt()) else bytes

        val decoded = try {
            TextDecoding.decode(content, isComplete = !isCutByBytes)
        } catch (error: TextDecoding.BinaryContentException) {
            throw DocumentReadException(Reason.UNSUPPORTED_FORMAT, error.message, error)
        }
        val isCutByChars = decoded.text.length > maxChars

        TextDocument(
            name = file.name,
            format = format,
            text = if (isCutByChars) decoded.text.take(maxChars) else decoded.text,
            charsetName = decoded.charsetName,
            isTruncated = isCutByBytes || isCutByChars
        )
    }

    private fun InputStream.readUpTo(limit: Long): ByteArray {
        val output = ByteArrayOutputStream()
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
