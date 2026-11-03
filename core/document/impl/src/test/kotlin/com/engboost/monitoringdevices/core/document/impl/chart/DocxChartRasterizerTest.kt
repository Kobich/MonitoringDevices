package com.engboost.monitoringdevices.core.document.impl.chart

import org.junit.Assert.assertArrayEquals
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

class DocxChartRasterizerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val renderedCharts = mutableListOf<Triple<ChartModel, Int, Int>>()
    private val rasterizer = DocxChartRasterizer(
        renderer = { chart, _, width, height ->
            renderedCharts += Triple(chart, width, height)
            PNG_BYTES
        }
    )

    @Test
    fun `replaces a chart with a picture of the same size`() {
        val input = docx(
            chartParts = mapOf("word/charts/chart1.xml" to ChartXmlFixtures.COLUMN_CHART),
            charts = listOf("rId5" to "charts/chart1.xml")
        )
        val output = temporaryFolder.newFile("out.docx")

        val replaced = rasterizer.rasterize(input, output)

        assertEquals(1, replaced)
        val (chart, width, height) = renderedCharts.single()
        assertEquals("Sales 2026", chart.title)
        assertEquals("5486400 EMU at 2x of 96 dpi", 1152, width)
        assertEquals(672, height)

        ZipFile(output).use { zip ->
            val document = zip.text("word/document.xml")
            assertFalse("chart reference is gone", document.contains("<c:chart"))
            assertTrue(document.contains("""graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture""""))
            assertTrue(document.contains("<pic:pic"))
            assertTrue(Regex("""r:embed="rIdChartImage1"""").containsMatchIn(document))
            assertTrue("drawing size is kept", document.contains("""cx="5486400""""))
            assertTrue("other content is kept", document.contains("Text before the chart"))

            val relationships = zip.text("word/_rels/document.xml.rels")
            assertTrue(relationships.contains("""Id="rIdChartImage1""""))
            assertTrue(relationships.contains("""Target="media/chart-image-1.png""""))

            assertTrue(zip.text("[Content_Types].xml").contains("""Extension="png""""))
            assertArrayEquals(PNG_BYTES, zip.getInputStream(zip.getEntry("word/media/chart-image-1.png")).readBytes())
            assertTrue("untouched parts are copied", zip.getEntry("word/charts/chart1.xml") != null)
        }
    }

    @Test
    fun `document without supported charts is left alone`() {
        val input = docx(
            chartParts = mapOf("word/charts/chart1.xml" to ChartXmlFixtures.UNSUPPORTED_CHART),
            charts = listOf("rId5" to "charts/chart1.xml")
        )
        val output = File(temporaryFolder.root, "out.docx")

        assertEquals(0, rasterizer.rasterize(input, output))
        assertFalse(output.exists())
    }

    @Test
    fun `uses the document theme for scheme colors`() {
        val input = docx(
            chartParts = mapOf(
                "word/charts/chart1.xml" to ChartXmlFixtures.COLUMN_CHART,
                "word/theme/theme1.xml" to ChartXmlFixtures.THEME
            ),
            charts = listOf("rId5" to "charts/chart1.xml"),
            extraRelationships = """<Relationship Id="rId9" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>"""
        )

        rasterizer.rasterize(input, temporaryFolder.newFile("out.docx"))

        assertEquals(0xFF112233.toInt(), renderedCharts.single().first.plots.single().series[0].color)
    }

    private fun docx(
        chartParts: Map<String, String>,
        charts: List<Pair<String, String>>,
        extraRelationships: String = ""
    ): File {
        val drawings = charts.joinToString("") { (id, _) ->
            """
            <w:p><w:r><w:drawing>
              <wp:inline distT="0" distB="0" distL="0" distR="0">
                <wp:extent cx="5486400" cy="3200400"/>
                <wp:docPr id="1" name="Chart 1"/>
                <a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/chart">
                  <c:chart r:id="$id"/>
                </a:graphicData></a:graphic>
              </wp:inline>
            </w:drawing></w:r></w:p>
            """
        }
        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <w:body><w:p><w:r><w:t>Text before the chart</w:t></w:r></w:p>$drawings</w:body>
            </w:document>"""
        val relationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              ${charts.joinToString("") { (id, target) -> """<Relationship Id="$id" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart" Target="$target"/>""" }}
              $extraRelationships
            </Relationships>"""
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>"""
        val packageRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>"""

        val file = temporaryFolder.newFile("in-${System.nanoTime()}.docx")
        ZipOutputStream(file.outputStream()).use { zip ->
            (mapOf(
                "[Content_Types].xml" to contentTypes,
                "_rels/.rels" to packageRelationships,
                "word/document.xml" to document,
                "word/_rels/document.xml.rels" to relationships
            ) + chartParts).forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun ZipFile.text(path: String) = getInputStream(getEntry(path)).readBytes().decodeToString()

    private companion object {
        val PNG_BYTES = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
    }
}
