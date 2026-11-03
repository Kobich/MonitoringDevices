package com.engboost.monitoringdevices.sample

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * TEMPORARY: builds small .docx files with tables, pictures and DrawingML charts for trying
 * the viewer. Remove together with [SampleFilesSeeder].
 */
internal object SampleDocx {

    class Chart(
        val type: Type,
        val title: String,
        val categories: List<String>,
        val series: List<Pair<String, List<Double>>>
    ) {
        enum class Type { COLUMN, BAR, LINE, PIE }
    }

    sealed interface Block {
        data class Heading(val text: String) : Block
        data class Paragraph(val text: String) : Block
        /** Small italic line above a picture or a chart, says what it is. */
        data class Caption(val text: String) : Block
        /** Embedded PNG shown at [widthCm] x [heightCm]. */
        class Picture(val png: ByteArray, val widthCm: Double, val heightCm: Double, val description: String) : Block
        data class Table(val rows: List<List<String>>) : Block
        data class ChartBlock(val chart: Chart) : Block
    }

    fun build(blocks: List<Block>): ByteArray {
        val charts = blocks.filterIsInstance<Block.ChartBlock>().map { it.chart }
        val pictures = blocks.filterIsInstance<Block.Picture>()
        val parts = linkedMapOf<String, ByteArray>(
            "[Content_Types].xml" to contentTypes(charts.size).toByteArray(),
            "_rels/.rels" to PACKAGE_RELATIONSHIPS.toByteArray(),
            "word/document.xml" to document(blocks).toByteArray(),
            "word/_rels/document.xml.rels" to documentRelationships(charts.size, pictures.size).toByteArray()
        )
        charts.forEachIndexed { index, chart -> parts["word/charts/chart${index + 1}.xml"] = chartPart(chart).toByteArray() }
        pictures.forEachIndexed { index, picture -> parts["word/media/image${index + 1}.png"] = picture.png }

        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            parts.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun document(blocks: List<Block>): String {
        var chartIndex = 0
        var pictureIndex = 0
        val body = blocks.joinToString("") { block ->
            when (block) {
                is Block.Caption -> """<w:p><w:pPr><w:spacing w:before="240" w:after="60"/></w:pPr><w:r><w:rPr><w:i/><w:color w:val="595959"/><w:sz w:val="20"/></w:rPr><w:t xml:space="preserve">${block.text.xml()}</w:t></w:r></w:p>"""
                is Block.Picture -> picture(++pictureIndex, block)
                is Block.Heading -> """<w:p><w:pPr><w:spacing w:after="120"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val="32"/></w:rPr><w:t>${block.text.xml()}</w:t></w:r></w:p>"""
                is Block.Paragraph -> """<w:p><w:r><w:t xml:space="preserve">${block.text.xml()}</w:t></w:r></w:p>"""
                is Block.Table -> table(block.rows)
                is Block.ChartBlock -> drawing(++chartIndex, block.chart.title)
            }
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart"><w:body>$body<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="850" w:bottom="1134" w:left="1701" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr></w:body></w:document>"""
    }

    private fun table(rows: List<List<String>>): String {
        val border = """w:val="single" w:sz="4" w:space="0" w:color="808080""""
        val content = rows.mapIndexed { rowIndex, cells ->
            val cellXml = cells.joinToString("") { cell ->
                val bold = if (rowIndex == 0) "<w:rPr><w:b/></w:rPr>" else ""
                """<w:tc><w:tcPr><w:tcW w:w="2400" w:type="dxa"/></w:tcPr><w:p><w:r>$bold<w:t>${cell.xml()}</w:t></w:r></w:p></w:tc>"""
            }
            "<w:tr>$cellXml</w:tr>"
        }.joinToString("")
        return """<w:tbl><w:tblPr><w:tblW w:w="0" w:type="auto"/><w:tblBorders><w:top $border/><w:left $border/><w:bottom $border/><w:right $border/><w:insideH $border/><w:insideV $border/></w:tblBorders></w:tblPr>$content</w:tbl><w:p/>"""
    }

    private fun picture(index: Int, picture: Block.Picture): String {
        val cx = (picture.widthCm * EMU_PER_CM).toLong()
        val cy = (picture.heightCm * EMU_PER_CM).toLong()
        return """<w:p><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="$cx" cy="$cy"/><wp:effectExtent l="0" t="0" r="0" b="0"/><wp:docPr id="${100 + index}" name="Picture $index" descr="${picture.description.xml()}"/><wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect="1"/></wp:cNvGraphicFramePr><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="0" name="image$index.png"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="rIdImage$index"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>"""
    }

    private fun drawing(index: Int, title: String) = """<w:p><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="5486400" cy="3200400"/><wp:effectExtent l="0" t="0" r="0" b="0"/><wp:docPr id="$index" name="Chart $index" descr="${title.xml()}"/><wp:cNvGraphicFramePr/><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/chart"><c:chart r:id="rIdChart$index"/></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>"""

    private fun chartPart(chart: Chart): String {
        val categories = chart.categories
        val series = chart.series.mapIndexed { index, (name, values) ->
            val color = if (chart.type == Chart.Type.PIE) "" else
                """<c:spPr>${if (chart.type == Chart.Type.LINE) """<a:ln w="28575"><a:solidFill><a:schemeClr val="accent${index + 1}"/></a:solidFill></a:ln>""" else """<a:solidFill><a:schemeClr val="accent${index + 1}"/></a:solidFill>"""}</c:spPr>"""
            val column = ('B' + index)
            """<c:ser><c:idx val="$index"/><c:order val="$index"/>""" +
                """<c:tx><c:strRef><c:f>Sheet1!${'$'}$column${'$'}1</c:f><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>${name.xml()}</c:v></c:pt></c:strCache></c:strRef></c:tx>""" +
                color +
                """<c:cat><c:strRef><c:f>Sheet1!${'$'}A${'$'}2:${'$'}A${'$'}${categories.size + 1}</c:f><c:strCache><c:ptCount val="${categories.size}"/>""" +
                categories.mapIndexed { i, category -> """<c:pt idx="$i"><c:v>${category.xml()}</c:v></c:pt>""" }.joinToString("") +
                """</c:strCache></c:strRef></c:cat>""" +
                """<c:val><c:numRef><c:f>Sheet1!${'$'}$column${'$'}2:${'$'}$column${'$'}${values.size + 1}</c:f><c:numCache><c:formatCode>General</c:formatCode><c:ptCount val="${values.size}"/>""" +
                values.mapIndexed { i, value -> """<c:pt idx="$i"><c:v>$value</c:v></c:pt>""" }.joinToString("") +
                """</c:numCache></c:numRef></c:val></c:ser>"""
        }.joinToString("")

        val plot = when (chart.type) {
            Chart.Type.COLUMN, Chart.Type.BAR -> {
                val direction = if (chart.type == Chart.Type.BAR) "bar" else "col"
                """<c:barChart><c:barDir val="$direction"/><c:grouping val="clustered"/><c:varyColors val="0"/>$series<c:gapWidth val="219"/><c:axId val="1"/><c:axId val="2"/></c:barChart>$AXES"""
            }

            Chart.Type.LINE -> """<c:lineChart><c:grouping val="standard"/><c:varyColors val="0"/>$series<c:marker val="1"/><c:axId val="1"/><c:axId val="2"/></c:lineChart>$AXES"""
            Chart.Type.PIE -> """<c:pieChart><c:varyColors val="1"/>$series<c:firstSliceAng val="0"/></c:pieChart>"""
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<c:chartSpace xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><c:roundedCorners val="0"/><c:chart><c:title><c:tx><c:rich><a:bodyPr/><a:p><a:r><a:t>${chart.title.xml()}</a:t></a:r></a:p></c:rich></c:tx><c:overlay val="0"/></c:title><c:autoTitleDeleted val="0"/><c:plotArea><c:layout/>$plot</c:plotArea><c:legend><c:legendPos val="b"/><c:overlay val="0"/></c:legend><c:plotVisOnly val="1"/></c:chart></c:chartSpace>"""
    }

    private fun contentTypes(chartCount: Int): String {
        val charts = (1..chartCount).joinToString("") { index ->
            """<Override PartName="/word/charts/chart$index.xml" ContentType="application/vnd.openxmlformats-officedocument.drawingml.chart+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>$charts</Types>"""
    }

    private fun documentRelationships(chartCount: Int, pictureCount: Int): String {
        val charts = (1..chartCount).joinToString("") { index ->
            """<Relationship Id="rIdChart$index" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart" Target="charts/chart$index.xml"/>"""
        }
        val pictures = (1..pictureCount).joinToString("") { index ->
            """<Relationship Id="rIdImage$index" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image$index.png"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$charts$pictures</Relationships>"""
    }

    private fun String.xml(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private const val EMU_PER_CM = 360_000.0

    private const val AXES =
        """<c:catAx><c:axId val="1"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/><c:axPos val="b"/><c:crossAx val="2"/></c:catAx><c:valAx><c:axId val="2"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/><c:axPos val="l"/><c:majorGridlines/><c:crossAx val="1"/></c:valAx>"""

    private const val PACKAGE_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
}
