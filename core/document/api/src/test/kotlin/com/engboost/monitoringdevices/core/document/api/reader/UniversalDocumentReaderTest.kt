package com.engboost.monitoringdevices.core.document.api.reader

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.core.document.api.reader.text.TextReader
import com.engboost.monitoringdevices.core.document.api.reader.word.DocxReader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UniversalDocumentReaderTest {
    private val docxReader = FakeDocxReader()
    private val textReader = FakeTextReader()
    private val reader = UniversalDocumentReader(listOf(docxReader, textReader))

    @Test
    fun `concrete readers declare their formats`() {
        assertEquals(setOf(DocumentFormat.DOCX), docxReader.formats)
        assertEquals(setOf(DocumentFormat.DOCX, DocumentFormat.TEXT), reader.formats)
    }

    @Test
    fun `delegates to the reader of the file format`() = runTest {
        assertSame(docxReader, reader.readerFor(source("a.docx")))
        assertTrue(reader.read(source("a.docx")) is HtmlDocument)
        assertTrue(reader.read(source("a.txt")) is TextDocument)
        assertNull(reader.readerFor(source("a.pdf")))
    }

    @Test
    fun `unsupported format fails with a reason`() = runTest {
        val error = runCatching { reader.read(source("a.pdf")) }.exceptionOrNull()

        assertEquals(
            DocumentReadException.Reason.UNSUPPORTED_FORMAT,
            (error as DocumentReadException).reason
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `two readers for one format are rejected`() {
        UniversalDocumentReader(listOf(docxReader, FakeDocxReader()))
    }

    private fun source(name: String) = DocumentSource.LocalFile(File(name))
}

private class FakeDocxReader : DocxReader {
    override suspend fun read(source: DocumentSource, options: ReadOptions) =
        HtmlDocument(source.name, DocumentFormat.DOCX, File("out"), emptyList())
}

private class FakeTextReader : TextReader {
    override suspend fun read(source: DocumentSource, options: ReadOptions) =
        TextDocument(source.name, DocumentFormat.TEXT, "", "UTF-8", isTruncated = false)
}
