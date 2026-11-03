package com.engboost.monitoringdevices.core.document.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class DocumentFormatTest {
    @Test
    fun `detects format by extension ignoring case`() {
        assertEquals(DocumentFormat.DOCX, DocumentFormat.of(File("Report.DOCX")))
        assertEquals(DocumentFormat.XLSX, DocumentFormat.of(File("budget.2026.xlsx")))
        assertEquals(DocumentFormat.JPEG, DocumentFormat.of(File("photo.jpeg")))
        assertEquals(DocumentFormat.JPEG, DocumentFormat.of(File("photo.JPG")))
        assertEquals(DocumentFormat.MARKDOWN, DocumentFormat.of(File("notes.md")))
        assertEquals(DocumentFormat.OGG, DocumentFormat.of(File("/music/track.oga")))
    }

    @Test
    fun `unknown files are unsupported`() {
        assertNull(DocumentFormat.of(File("archive.zip")))
        assertNull(DocumentFormat.of(File("no_extension")))
        assertNull(DocumentFormat.of(File(".hidden")))
    }

    @Test
    fun `every extension belongs to one format`() {
        val extensions = DocumentFormat.entries.flatMap { it.extensions }

        assertEquals(extensions.size, extensions.toSet().size)
    }
}
