package com.engboost.monitoringdevices.core.document.impl.chart

import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_CHART
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_DRAWING
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_PICTURE
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_RELATIONSHIPS
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_WORD_DRAWING
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_XMLNS
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.ancestor
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.child
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.childElements
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.descendants
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlPackage
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile

/**
 * Each chart of the main document becomes a picture of the same size.
 * Charts we can't parse stay untouched.
 */
internal class DocxChartRasterizer(
    private val renderer: ChartImageRenderer,
    private val parser: ChartXmlParser = ChartXmlParser()
) : ChartRasterizer {

    override fun rasterize(input: File, output: File): Int {
        ZipFile(input).use { zip ->
            val ooxml = OoxmlPackage(zip)
            val documentPath = ooxml.mainPartPath(fallback = "word/document.xml")
            val document = ooxml.part(documentPath)
            val relationships = ooxml.relationships(documentPath)
            val theme = ooxml.themeOf(documentPath)?.let(parser::parseTheme) ?: ChartTheme()

            var replaced = 0
            document.documentElement.descendants(NS_DRAWING, "graphicData")
                .filter { element -> element.getAttribute("uri") == NS_CHART }
                .forEach { graphicData ->
                    val chartPath = relationships.target(graphicData.child(NS_CHART, "chart")?.getAttributeNS(NS_RELATIONSHIPS, "id"))
                    val chart = chartPath?.let(ooxml::partOrNull)?.let { part -> parser.parse(part, theme) } ?: return@forEach
                    val (cx, cy) = graphicData.extent() ?: DEFAULT_EXTENT

                    val image = renderer.render(chart, theme, ChartScale.emuToPx(cx), ChartScale.emuToPx(cy))
                    replaced++
                    graphicData.replaceWithPicture(document, ooxml.addImage(documentPath, image), cx, cy, replaced)
                }

            if (replaced == 0) return 0
            ooxml.markModified(documentPath)
            ooxml.writeTo(output)
            return replaced
        }
    }

    private fun Element.extent(): Pair<Long, Long>? {
        val drawing = ancestor(NS_WORD_DRAWING, "inline", "anchor") ?: return null
        val extent = drawing.child(NS_WORD_DRAWING, "extent") ?: return null
        val cx = extent.getAttribute("cx").toLongOrNull() ?: return null
        val cy = extent.getAttribute("cy").toLongOrNull() ?: return null
        return (cx to cy).takeIf { cx > 0 && cy > 0 }
    }

    private fun Element.replaceWithPicture(document: Document, imageId: String, cx: Long, cy: Long, index: Int) {
        childElements().forEach { child -> removeChild(child) }
        setAttribute("uri", NS_PICTURE)

        fun drawing(name: String) = document.createElementNS(NS_DRAWING, "a:$name")
        fun picture(name: String) = document.createElementNS(NS_PICTURE, "pic:$name")

        val pic = picture("pic").apply {
            setAttributeNS(NS_XMLNS, "xmlns:pic", NS_PICTURE)
            setAttributeNS(NS_XMLNS, "xmlns:a", NS_DRAWING)
            setAttributeNS(NS_XMLNS, "xmlns:r", NS_RELATIONSHIPS)
        }
        pic.appendChild(picture("nvPicPr").apply {
            appendChild(picture("cNvPr").apply {
                setAttribute("id", "0")
                setAttribute("name", "Chart $index")
            })
            appendChild(picture("cNvPicPr"))
        })
        pic.appendChild(picture("blipFill").apply {
            appendChild(drawing("blip").apply { setAttributeNS(NS_RELATIONSHIPS, "r:embed", imageId) })
            appendChild(drawing("stretch").apply { appendChild(drawing("fillRect")) })
        })
        pic.appendChild(picture("spPr").apply {
            appendChild(drawing("xfrm").apply {
                appendChild(drawing("off").apply {
                    setAttribute("x", "0")
                    setAttribute("y", "0")
                })
                appendChild(drawing("ext").apply {
                    setAttribute("cx", cx.toString())
                    setAttribute("cy", cy.toString())
                })
            })
            appendChild(drawing("prstGeom").apply {
                setAttribute("prst", "rect")
                appendChild(drawing("avLst"))
            })
        })
        appendChild(pic)
    }

    private companion object {
        /** 15 x 9 cm, the size of a chart inserted into Word with default settings. */
        val DEFAULT_EXTENT = 5486400L to 3200400L
    }
}
