package com.android.core.document.impl.office

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class DocxTabStopsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val patch = DocxTabStops()

    @Test
    fun `stops of the style and the paragraph are merged, clear removes one`() {
        val output = patched(
            paragraph(
                properties = """<w:pStyle w:val="Toc"/><w:tabs><w:tab w:val="center" w:pos="4819"/><w:tab w:val="clear" w:pos="2000"/></w:tabs>""",
                runs = TAB_ELEMENT
            )
        )

        // Toc: right 9638 with dots and left 2000; the paragraph adds a centered 4819 and clears 2000.
        assertTrue(output, "_OdrTabs_720_C4819nR9638d_" in output)
    }

    @Test
    fun `kinds and leaders are encoded, positions in order`() {
        val output = patched(
            paragraph(
                properties = """<w:tabs><w:tab w:val="decimal" w:pos="4000"/><w:tab w:val="left" w:pos="1000" w:leader="hyphen"/><w:tab w:val="end" w:pos="9000" w:leader="underscore"/><w:tab w:val="bar" w:pos="3000"/></w:tabs>""",
                runs = TAB_ELEMENT
            )
        )

        assertTrue(output, "_OdrTabs_720_L1000hD4000nR9000u_" in output)
    }

    @Test
    fun `a tab character counts like a tab element`() {
        val output = patched(paragraph(properties = """<w:tabs><w:tab w:val="right" w:pos="9000"/></w:tabs>""", runs = "<w:r><w:t>a\tb</w:t></w:r>"))

        assertTrue(output, "_OdrTabs_720_R9000n_" in output)
    }

    @Test
    fun `paragraphs without tabs or without stops are left alone`() {
        val input = docx(
            paragraph(properties = """<w:tabs><w:tab w:val="right" w:pos="9000"/></w:tabs>""", runs = "<w:r><w:t>no tab</w:t></w:r>"),
            paragraph(properties = "", runs = TAB_ELEMENT)
        )

        assertFalse(patch.apply(input, File(folder.root, "out.docx")))
    }

    @Test
    fun `bookmark ids stay unique when both patches run`() {
        val indented = File(folder.root, "indented.docx")
        val both = File(folder.root, "both.docx")
        val input = docx(paragraph(properties = """<w:ind w:firstLine="709"/><w:tabs><w:tab w:val="right" w:pos="9000"/></w:tabs>""", runs = TAB_ELEMENT))

        assertTrue(DocxFirstLineIndents().apply(input, indented))
        assertTrue(patch.apply(indented, both))

        val ids = Regex("""bookmarkStart[^>]*w:id="(\d+)"""").findAll(documentXml(both)).map { it.groupValues[1] }.toList()
        assertEquals(2, ids.size)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `the page gets the script and a style for the tab`() {
        assertTrue(patch.script.contains("_OdrTabs_"))
        assertTrue(patch.css(emptySequence()).contains(".odr-tab"))
    }

    private fun patched(vararg paragraphs: String): String {
        val output = File(folder.root, "patched.docx")
        assertTrue(patch.apply(docx(*paragraphs), output))
        return documentXml(output)
    }

    private fun documentXml(file: File): String {
        return ZipFile(file).use { zip -> zip.getInputStream(zip.getEntry("word/document.xml")).reader().readText() }
    }

    private fun paragraph(properties: String, runs: String): String {
        val pPr = if (properties.isEmpty()) "" else "<w:pPr>$properties</w:pPr>"
        return "<w:p>$pPr$runs</w:p>"
    }

    private fun docx(vararg paragraphs: String): File {
        val file = folder.newFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            fun put(path: String, text: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
            put("[Content_Types].xml", CONTENT_TYPES)
            put("_rels/.rels", PACKAGE_RELATIONSHIPS)
            put("word/_rels/document.xml.rels", DOCUMENT_RELATIONSHIPS)
            put("word/document.xml", """<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="$W"><w:body>${paragraphs.joinToString("")}</w:body></w:document>""")
            put("word/styles.xml", STYLES)
            put("word/settings.xml", """<?xml version="1.0" encoding="UTF-8"?><w:settings xmlns:w="$W"><w:defaultTabStop w:val="720"/></w:settings>""")
        }
        return file
    }

    private companion object {
        const val W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
        const val TAB_ELEMENT = "<w:r><w:t>a</w:t></w:r><w:r><w:tab/><w:t>b</w:t></w:r>"

        const val STYLES = """<?xml version="1.0" encoding="UTF-8"?><w:styles xmlns:w="$W">
            <w:style w:type="paragraph" w:default="1" w:styleId="Normal"/>
            <w:style w:type="paragraph" w:styleId="Toc"><w:basedOn w:val="Normal"/><w:pPr><w:tabs><w:tab w:val="right" w:leader="dot" w:pos="9638"/><w:tab w:val="left" w:pos="2000"/></w:tabs></w:pPr></w:style>
            </w:styles>"""

        const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/></Types>"""

        const val PACKAGE_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""

        const val DOCUMENT_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/></Relationships>"""
    }
}
