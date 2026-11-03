package com.engboost.monitoringdevices.core.document.impl

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException

class CompositeDocumentReaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val textReader = FakeReader(setOf(DocumentFormat.TXT, DocumentFormat.JSON))
    private val reader = CompositeDocumentReader(listOf(textReader))

    @Test
    fun `passes the detected format to the reader`() = runBlocking {
        val document = reader.read(temporaryFolder.newFile("data.JSON"))

        assertEquals(DocumentFormat.JSON, document.format)
    }

    @Test
    fun `missing file`() {
        assertReason(Reason.NOT_FOUND) { reader.read(File(temporaryFolder.root, "missing.txt")) }
    }

    @Test
    fun `unknown and unregistered formats are unsupported`() {
        assertReason(Reason.UNSUPPORTED_FORMAT) { reader.read(temporaryFolder.newFile("archive.zip")) }
        assertReason(Reason.UNSUPPORTED_FORMAT) { reader.read(temporaryFolder.newFile("report.docx")) }
    }

    @Test
    fun `other errors of a reader become DocumentReadException`() {
        textReader.error = FileNotFoundException("gone")
        assertReason(Reason.IO_ERROR) { reader.read(temporaryFolder.newFile("a.txt")) }

        textReader.error = OutOfMemoryError()
        assertReason(Reason.TOO_LARGE) { reader.read(temporaryFolder.newFile("b.txt")) }

        textReader.error = DocumentReadException(Reason.CORRUPTED)
        assertReason(Reason.CORRUPTED) { reader.read(temporaryFolder.newFile("c.txt")) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `one format can't have two readers`() {
        CompositeDocumentReader(listOf(FakeReader(setOf(DocumentFormat.TXT)), FakeReader(setOf(DocumentFormat.TXT))))
    }

    private fun assertReason(reason: Reason, block: suspend () -> Unit) {
        try {
            runBlocking { block() }
            fail("Expected $reason")
        } catch (error: DocumentReadException) {
            assertEquals(reason, error.reason)
        }
    }

    private class FakeReader(override val formats: Set<DocumentFormat>) : FormatReader<TextDocument> {
        var error: Throwable? = null

        override suspend fun read(file: File, format: DocumentFormat, password: String?): TextDocument {
            error?.let { throw it }
            return TextDocument(name = file.name, format = format, text = "", charsetName = "UTF-8", isTruncated = false)
        }
    }
}
