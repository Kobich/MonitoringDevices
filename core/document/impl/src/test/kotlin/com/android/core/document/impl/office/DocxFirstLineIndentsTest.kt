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

class DocxFirstLineIndentsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val patch = DocxFirstLineIndents()

    @Test
    fun `direct, style, inherited and default indents get a bookmark`() {
        val output = patchedDocument(
            paragraph("direct", properties = """<w:ind w:firstLine="1440"/>"""),
            paragraph("hanging", properties = """<w:ind w:left="1440" w:hanging="720"/>"""),
            paragraph("style", properties = """<w:pStyle w:val="Child"/>"""),
            paragraph("default")
        )

        assertTrue(output, "_OdrFirstLine1440_" in output)
        assertTrue(output, "_OdrHanging720_" in output)
        // Child is based on Indented (1080); the plain paragraph gets docDefaults (709) through Normal.
        assertTrue(output, "_OdrFirstLine1080_" in output)
        assertTrue(output, "_OdrFirstLine709_" in output)
    }

    @Test
    fun `list items and a reset to zero are left alone`() {
        val output = patchedDocument(
            paragraph("list", properties = """<w:numPr><w:ilvl w:val="0"/><w:numId w:val="1"/></w:numPr>"""),
            paragraph("reset", properties = """<w:ind w:firstLine="0"/>"""),
            paragraph("default")
        )

        assertEquals(1, Regex("_OdrFirstLine709_").findAll(output).count())
        assertFalse(output, "_OdrFirstLine0_" in output)
    }

    @Test
    fun `a document without indents is not rewritten`() {
        val input = docx(STYLES_WITHOUT_DEFAULT_INDENT, paragraph("plain"))
        val output = File(folder.root, "out.docx")

        assertFalse(patch.apply(input, output))
        assertFalse(output.exists())
    }

    @Test
    fun `css has one rule per indent, hanging ones negative`() {
        val html = """<x-p><a id="_OdrFirstLine709_0"></a>a</x-p><x-p><a id="_OdrFirstLine709_1"></a>b</x-p>""" +
            """<x-p><a id="_OdrHanging360_2"></a>c</x-p>"""

        val css = patch.css(sequenceOf(html))

        assertEquals(
            """x-p:has(>a[id^="_OdrFirstLine709_"]){text-indent:35.45pt}""" +
                """x-p:has(>a[id^="_OdrHanging360_"]){text-indent:-18.0pt}""",
            css
        )
        assertEquals("", patch.css(sequenceOf("<x-p>no marks</x-p>")))
    }

    private fun patchedDocument(vararg paragraphs: String): String {
        val output = File(folder.root, "patched.docx")
        assertTrue(patch.apply(docx(STYLES, *paragraphs), output))
        return ZipFile(output).use { zip -> zip.getInputStream(zip.getEntry("word/document.xml")).reader().readText() }
    }

    private fun paragraph(text: String, properties: String = ""): String {
        val pPr = if (properties.isEmpty()) "" else "<w:pPr>$properties</w:pPr>"
        return """<w:p>$pPr<w:r><w:t>$text</w:t></w:r></w:p>"""
    }

    private fun docx(styles: String, vararg paragraphs: String): File {
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
            put("word/styles.xml", styles)
        }
        return file
    }

    private companion object {
        const val W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

        const val STYLES = """<?xml version="1.0" encoding="UTF-8"?><w:styles xmlns:w="$W">
            <w:docDefaults><w:pPrDefault><w:pPr><w:ind w:firstLine="709"/></w:pPr></w:pPrDefault></w:docDefaults>
            <w:style w:type="paragraph" w:default="1" w:styleId="Normal"/>
            <w:style w:type="paragraph" w:styleId="Indented"><w:basedOn w:val="Normal"/><w:pPr><w:ind w:firstLine="1080"/></w:pPr></w:style>
            <w:style w:type="paragraph" w:styleId="Child"><w:basedOn w:val="Indented"/></w:style>
            </w:styles>"""

        const val STYLES_WITHOUT_DEFAULT_INDENT =
            """<?xml version="1.0" encoding="UTF-8"?><w:styles xmlns:w="$W"><w:style w:type="paragraph" w:default="1" w:styleId="Normal"/></w:styles>"""

        const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/></Types>"""

        const val PACKAGE_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""

        const val DOCUMENT_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    }
}
