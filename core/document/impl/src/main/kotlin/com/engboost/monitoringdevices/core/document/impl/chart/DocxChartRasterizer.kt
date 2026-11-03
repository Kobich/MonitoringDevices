package com.engboost.monitoringdevices.core.document.impl.chart

import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.NS_CHART
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.NS_CONTENT_TYPES
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.NS_DRAWING
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.NS_PACKAGE_RELATIONSHIPS
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.NS_PICTURE
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.NS_RELATIONSHIPS
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.NS_WORD_DRAWING
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.NS_XMLNS
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.REL_IMAGE
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.REL_OFFICE_DOCUMENT
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.REL_THEME
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.ancestor
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.child
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.childElements
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.children
import com.engboost.monitoringdevices.core.document.impl.chart.OoxmlXml.descendants
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Writes a copy of a .docx where every chart of the main document is replaced with
 * a picture drawn by [renderer], keeping its size and position. Charts we can't parse
 * stay untouched.
 */
internal class DocxChartRasterizer(
    private val renderer: ChartImageRenderer,
    private val parser: ChartXmlParser = ChartXmlParser()
) {
    /** @return number of replaced charts; [output] is written only when it is above zero. */
    fun rasterize(input: File, output: File): Int {
        ZipFile(input).use { zip ->
            val documentPath = mainDocumentPath(zip)
            val relationshipsPath = OoxmlXml.relationshipsPath(documentPath)
            val document = zip.readXml(documentPath)
            val relationships = zip.readXml(relationshipsPath)
            val contentTypes = zip.readXml(CONTENT_TYPES_PATH)

            val targets = relationships.documentElement.children(NS_PACKAGE_RELATIONSHIPS, "Relationship")
                .associate { relationship ->
                    relationship.getAttribute("Id") to relationship
                }
            val theme = targets.values
                .firstOrNull { relationship -> relationship.getAttribute("Type") == REL_THEME }
                ?.let { relationship -> zip.readXmlOrNull(resolve(documentPath, relationship)) }
                ?.let(parser::parseTheme)
                ?: ChartTheme()

            val newImages = linkedMapOf<String, ByteArray>()
            val usedIds = targets.keys.toMutableSet()
            val graphicData = document.documentElement.descendants(NS_DRAWING, "graphicData")
                .filter { element -> element.getAttribute("uri") == NS_CHART }

            graphicData.forEach { element ->
                val relationshipId = element.child(NS_CHART, "chart")?.getAttributeNS(NS_RELATIONSHIPS, "id")
                val chartRelationship = targets[relationshipId] ?: return@forEach
                val chartPart = zip.readXmlOrNull(resolve(documentPath, chartRelationship)) ?: return@forEach
                val chart = parser.parse(chartPart, theme) ?: return@forEach
                val extent = element.extent() ?: DEFAULT_EXTENT

                val image = renderer.render(chart, theme, extent.widthPx(), extent.heightPx())
                val index = newImages.size + 1
                val mediaTarget = uniqueName(zip, documentPath, index)
                val imageId = generateSequence(index) { it + 1 }
                    .map { candidate -> "rIdChartImage$candidate" }
                    .first { candidate -> candidate !in usedIds }
                usedIds += imageId

                relationships.documentElement.appendChild(
                    relationships.createElementNS(NS_PACKAGE_RELATIONSHIPS, "Relationship").apply {
                        setAttribute("Id", imageId)
                        setAttribute("Type", REL_IMAGE)
                        setAttribute("Target", mediaTarget)
                    }
                )
                newImages[OoxmlXml.resolveTarget(documentPath, mediaTarget)] = image
                element.replaceWithPicture(document, imageId, extent, index)
            }

            if (newImages.isEmpty()) return 0
            contentTypes.ensureDefault(extension = "png", contentType = "image/png")

            val replaced = mapOf(
                documentPath to OoxmlXml.serialize(document),
                relationshipsPath to OoxmlXml.serialize(relationships),
                CONTENT_TYPES_PATH to OoxmlXml.serialize(contentTypes)
            )
            writeCopy(zip, output, replaced, newImages)
            return newImages.size
        }
    }

    private fun mainDocumentPath(zip: ZipFile): String {
        val packageRelationships = zip.readXmlOrNull(PACKAGE_RELATIONSHIPS_PATH)
        val target = packageRelationships?.documentElement
            ?.children(NS_PACKAGE_RELATIONSHIPS, "Relationship")
            ?.firstOrNull { relationship -> relationship.getAttribute("Type") == REL_OFFICE_DOCUMENT }
            ?.getAttribute("Target")
        return target?.let { OoxmlXml.resolveTarget("", it) } ?: DEFAULT_DOCUMENT_PATH
    }

    private fun resolve(partPath: String, relationship: Element): String {
        return OoxmlXml.resolveTarget(partPath, relationship.getAttribute("Target"))
    }

    private fun uniqueName(zip: ZipFile, documentPath: String, index: Int): String {
        return generateSequence(index) { it + 1 }
            .map { candidate -> "media/chart-image-$candidate.png" }
            .first { candidate -> zip.getEntry(OoxmlXml.resolveTarget(documentPath, candidate)) == null }
    }

    /** Size of the drawing in EMU (`wp:extent`), 914400 per inch. */
    private class Extent(val cx: Long, val cy: Long) {
        fun widthPx(): Int = toPx(cx)
        fun heightPx(): Int = toPx(cy)

        private fun toPx(emu: Long): Int {
            val px = emu / EMU_PER_PX * ChartScale.RENDER_SCALE
            return px.toInt().coerceIn(MIN_SIDE_PX, MAX_SIDE_PX)
        }
    }

    private fun Element.extent(): Extent? {
        val drawing = ancestor(NS_WORD_DRAWING, "inline", "anchor") ?: return null
        val extent = drawing.child(NS_WORD_DRAWING, "extent") ?: return null
        val cx = extent.getAttribute("cx").toLongOrNull() ?: return null
        val cy = extent.getAttribute("cy").toLongOrNull() ?: return null
        return Extent(cx, cy).takeIf { cx > 0 && cy > 0 }
    }

    /** Turns `a:graphicData` with a chart into the one Word writes for a picture. */
    private fun Element.replaceWithPicture(document: Document, imageId: String, extent: Extent, index: Int) {
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
            appendChild(drawing("blip").apply {
                setAttributeNS(NS_RELATIONSHIPS, "r:embed", imageId)
            })
            appendChild(drawing("stretch").apply { appendChild(drawing("fillRect")) })
        })
        pic.appendChild(picture("spPr").apply {
            appendChild(drawing("xfrm").apply {
                appendChild(drawing("off").apply {
                    setAttribute("x", "0")
                    setAttribute("y", "0")
                })
                appendChild(drawing("ext").apply {
                    setAttribute("cx", extent.cx.toString())
                    setAttribute("cy", extent.cy.toString())
                })
            })
            appendChild(drawing("prstGeom").apply {
                setAttribute("prst", "rect")
                appendChild(drawing("avLst"))
            })
        })
        appendChild(pic)
    }

    private fun Document.ensureDefault(extension: String, contentType: String) {
        val root = documentElement
        val exists = root.children(NS_CONTENT_TYPES, "Default")
            .any { element -> element.getAttribute("Extension").equals(extension, ignoreCase = true) }
        if (!exists) {
            root.insertBefore(
                createElementNS(NS_CONTENT_TYPES, "Default").apply {
                    setAttribute("Extension", extension)
                    setAttribute("ContentType", contentType)
                },
                root.firstChild
            )
        }
    }

    private fun writeCopy(
        zip: ZipFile,
        output: File,
        replaced: Map<String, ByteArray>,
        added: Map<String, ByteArray>
    ) {
        ZipOutputStream(output.outputStream().buffered()).use { out ->
            zip.entries().asSequence().forEach { entry ->
                out.putNextEntry(ZipEntry(entry.name))
                val replacement = replaced[entry.name]
                if (replacement != null) {
                    out.write(replacement)
                } else {
                    zip.getInputStream(entry).use { input -> input.copyTo(out) }
                }
                out.closeEntry()
            }
            added.forEach { (path, bytes) ->
                out.putNextEntry(ZipEntry(path))
                out.write(bytes)
                out.closeEntry()
            }
        }
    }

    private fun ZipFile.readXml(path: String): Document {
        return readXmlOrNull(path) ?: throw IOException("$path is missing")
    }

    private fun ZipFile.readXmlOrNull(path: String): Document? {
        val entry = getEntry(path) ?: return null
        if (entry.size > MAX_XML_PART_BYTES) throw IOException("$path is too large")
        return getInputStream(entry).use(OoxmlXml::parse)
    }

    private companion object {
        const val CONTENT_TYPES_PATH = "[Content_Types].xml"
        const val PACKAGE_RELATIONSHIPS_PATH = "_rels/.rels"
        const val DEFAULT_DOCUMENT_PATH = "word/document.xml"
        const val MAX_XML_PART_BYTES = 64L * 1024 * 1024

        const val EMU_PER_PX = 9525.0 // 914400 EMU per inch / 96 px per inch
        const val MIN_SIDE_PX = 64
        const val MAX_SIDE_PX = 2400

        /** 15 x 9 cm, the size of a chart inserted into Word with default settings. */
        val DEFAULT_EXTENT = Extent(5486400, 3200400)
    }
}
