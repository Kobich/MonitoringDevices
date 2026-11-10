package com.android.core.document.impl.chart

import android.util.Log
import com.android.core.document.impl.office.DocumentPatch
import com.android.core.document.impl.ooxml.OoxmlPackage
import com.android.core.document.impl.ooxml.OoxmlXml.NS_CHART
import com.android.core.document.impl.ooxml.OoxmlXml.NS_DRAWING
import com.android.core.document.impl.ooxml.OoxmlXml.NS_RELATIONSHIPS
import com.android.core.document.impl.ooxml.OoxmlXml.NS_XMLNS
import com.android.core.document.impl.ooxml.OoxmlXml.child
import com.android.core.document.impl.ooxml.OoxmlXml.descendants
import com.android.core.document.impl.ooxml.OoxmlXml.path
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile

private const val TAG = "PptxChartRasterizer"

/** Charts on slide layouts and masters, and charts we can't parse, stay untouched. */
internal class PptxChartRasterizer(
    private val renderer: ChartImageRenderer,
    private val parser: ChartXmlParser = ChartXmlParser()
) : DocumentPatch {

    override fun apply(input: File, output: File): Boolean {
        ZipFile(input).use { zip ->
            val ooxml = OoxmlPackage(zip)
            val presentationPath = ooxml.mainPartPath(fallback = "ppt/presentation.xml")
            val theme = ooxml.themeOf(presentationPath)?.let(parser::parseTheme) ?: ChartTheme()

            var replaced = 0
            ooxml.relationships(presentationPath).targetsOfType(REL_SLIDE).forEach { slidePath ->
                val slide = ooxml.partOrNull(slidePath) ?: return@forEach
                val slideRelationships = ooxml.relationships(slidePath)
                var isSlideChanged = false

                slide.documentElement.descendants(NS_PRESENTATION, "graphicFrame").forEach { frame ->
                    val graphicData = frame.path(NS_DRAWING, "graphic", "graphicData")
                        ?.takeIf { it.getAttribute("uri") == NS_CHART }
                        ?: return@forEach
                    val chartPath = slideRelationships.target(
                        graphicData.child(NS_CHART, "chart")?.getAttributeNS(NS_RELATIONSHIPS, "id")
                    )
                    val chart = chartPath?.let(ooxml::partOrNull)?.let { part -> parser.parse(part, theme) }
                        ?: return@forEach
                    val transform = frame.child(NS_PRESENTATION, "xfrm") ?: return@forEach
                    val offset = transform.child(NS_DRAWING, "off")
                    val extent = transform.child(NS_DRAWING, "ext") ?: return@forEach
                    val cx = extent.getAttribute("cx").toLongOrNull()?.takeIf { it > 0 } ?: return@forEach
                    val cy = extent.getAttribute("cy").toLongOrNull()?.takeIf { it > 0 } ?: return@forEach

                    val image = renderer.render(chart, theme, ChartScale.emuToPx(cx), ChartScale.emuToPx(cy))
                    val imageId = ooxml.addImage(slidePath, image)
                    val properties = frame.path(NS_PRESENTATION, "nvGraphicFramePr", "cNvPr")
                    val picture = slide.picture(
                        id = properties?.getAttribute("id")?.takeIf { it.isNotEmpty() } ?: "${10_000 + replaced}",
                        name = properties?.getAttribute("name")?.takeIf { it.isNotEmpty() } ?: "Chart ${replaced + 1}",
                        imageId = imageId,
                        x = offset?.getAttribute("x") ?: "0",
                        y = offset?.getAttribute("y") ?: "0",
                        cx = cx,
                        cy = cy
                    )
                    frame.parentNode.replaceChild(picture, frame)
                    replaced++
                    isSlideChanged = true
                }
                if (isSlideChanged) ooxml.markModified(slidePath)
            }

            if (replaced == 0) return false
            ooxml.writeTo(output)
            Log.d(TAG, "Replaced $replaced chart(s)")
            return true
        }
    }

    private fun Document.picture(
        id: String,
        name: String,
        imageId: String,
        x: String,
        y: String,
        cx: Long,
        cy: Long
    ): Element {
        fun presentation(tag: String) = createElementNS(NS_PRESENTATION, "p:$tag")
        fun drawing(tag: String) = createElementNS(NS_DRAWING, "a:$tag")

        return presentation("pic").apply {
            setAttributeNS(NS_XMLNS, "xmlns:p", NS_PRESENTATION)
            setAttributeNS(NS_XMLNS, "xmlns:a", NS_DRAWING)
            setAttributeNS(NS_XMLNS, "xmlns:r", NS_RELATIONSHIPS)

            appendChild(presentation("nvPicPr").apply {
                appendChild(presentation("cNvPr").apply {
                    setAttribute("id", id)
                    setAttribute("name", name)
                })
                appendChild(presentation("cNvPicPr").apply {
                    appendChild(drawing("picLocks").apply { setAttribute("noChangeAspect", "1") })
                })
                appendChild(presentation("nvPr"))
            })
            appendChild(presentation("blipFill").apply {
                appendChild(drawing("blip").apply { setAttributeNS(NS_RELATIONSHIPS, "r:embed", imageId) })
                appendChild(drawing("stretch").apply { appendChild(drawing("fillRect")) })
            })
            appendChild(presentation("spPr").apply {
                appendChild(drawing("xfrm").apply {
                    appendChild(drawing("off").apply {
                        setAttribute("x", x)
                        setAttribute("y", y)
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
        }
    }

    private companion object {
        const val NS_PRESENTATION = "http://schemas.openxmlformats.org/presentationml/2006/main"
        const val REL_SLIDE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide"
    }
}
