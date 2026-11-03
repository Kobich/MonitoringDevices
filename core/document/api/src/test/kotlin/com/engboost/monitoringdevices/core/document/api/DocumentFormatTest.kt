package com.engboost.monitoringdevices.core.document.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class DocumentFormatTest {
    @Test
    fun `detects format by extension ignoring case`() {
        assertEquals(DocumentFormat.DOCX, DocumentFormat.of("Report.DOCX"))
        assertEquals(DocumentFormat.XLSX, DocumentFormat.of("budget.2026.xlsx"))
        assertEquals(DocumentFormat.IMAGE, DocumentFormat.of("photo.jpeg"))
        assertEquals(DocumentFormat.TEXT, DocumentFormat.of("notes.md"))
    }

    @Test
    fun `specific mime type wins over extension`() {
        assertEquals(DocumentFormat.PDF, DocumentFormat.of("download", "application/pdf"))
        assertEquals(DocumentFormat.CSV, DocumentFormat.of("data.txt", "text/csv; charset=utf-8"))
    }

    @Test
    fun `generic mime type falls back to extension`() {
        assertEquals(DocumentFormat.ODT, DocumentFormat.of("letter.odt", "application/octet-stream"))
    }

    @Test
    fun `unknown files are unsupported`() {
        assertNull(DocumentFormat.of("archive.zip"))
        assertNull(DocumentFormat.of("no_extension"))
        assertNull(DocumentFormat.of(".hidden"))
    }

    @Test
    fun `local file source uses its name`() {
        assertEquals(DocumentFormat.PPTX, DocumentFormat.of(DocumentSource.LocalFile(File("/a/b/slides.pptx"))))
    }

    @Test
    fun `every extension and mime type belongs to one format`() {
        val extensions = DocumentFormat.entries.flatMap { it.extensions }
        val mimeTypes = DocumentFormat.entries.flatMap { it.mimeTypes }

        assertEquals(extensions.size, extensions.toSet().size)
        assertEquals(mimeTypes.size, mimeTypes.toSet().size)
    }
}
