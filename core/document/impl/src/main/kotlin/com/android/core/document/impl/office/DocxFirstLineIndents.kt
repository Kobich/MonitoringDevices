package com.android.core.document.impl.office

import android.util.Log
import com.android.core.document.impl.ooxml.OoxmlPackage
import com.android.core.document.impl.ooxml.OoxmlXml.NS_WORD
import com.android.core.document.impl.ooxml.OoxmlXml.REL_STYLES
import com.android.core.document.impl.ooxml.OoxmlXml.child
import com.android.core.document.impl.ooxml.OoxmlXml.children
import com.android.core.document.impl.ooxml.OoxmlXml.descendants
import com.android.core.document.impl.ooxml.OoxmlXml.path
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile
import kotlin.math.roundToInt

private const val TAG = "DocxFirstLineIndents"

/**
 * OpenDocument.core drops first-line and hanging indents: every line of a paragraph starts at its left
 * indent. Each such paragraph gets a hidden bookmark at its start that names the indent; the library
 * keeps it as `<a id>` at the start of the paragraph, and [css] turns it into `text-indent`.
 * List items are skipped: the library lays them out on its own.
 */
internal class DocxFirstLineIndents : DocumentPatch {

    override fun apply(input: File, output: File): Boolean {
        ZipFile(input).use { zip ->
            val ooxml = OoxmlPackage(zip)
            val documentPath = ooxml.mainPartPath(fallback = "word/document.xml")
            val document = ooxml.part(documentPath)
            val styles = ooxml.relationships(documentPath).firstTargetOfType(REL_STYLES)?.let(ooxml::partOrNull)
            val indents = IndentResolver(styles)

            var marked = 0
            document.documentElement.descendants(NS_WORD, "p").forEach { paragraph ->
                val twips = indents.firstLineTwips(paragraph) ?: return@forEach
                paragraph.insertBookmark(document, name = bookmarkName(twips, marked), id = BOOKMARK_ID_BASE + marked)
                marked++
            }

            if (marked == 0) return false
            ooxml.markModified(documentPath)
            ooxml.writeTo(output)
            Log.d(TAG, "Marked $marked paragraph(s)")
            return true
        }
    }

    private fun Element.insertBookmark(document: Document, name: String, id: Int) {
        val start = document.createElementNS(NS_WORD, "w:bookmarkStart").apply {
            setAttributeNS(NS_WORD, "w:id", id.toString())
            setAttributeNS(NS_WORD, "w:name", name)
        }
        val end = document.createElementNS(NS_WORD, "w:bookmarkEnd").apply {
            setAttributeNS(NS_WORD, "w:id", id.toString())
        }
        val properties = child(NS_WORD, "pPr")
        val before = if (properties != null) properties.nextSibling else firstChild
        insertBefore(start, before)
        insertBefore(end, before)
    }

    companion object {
        private const val FIRST_LINE = "_OdrFirstLine"
        private const val HANGING = "_OdrHanging"

        /** Above the ids Word gives its own bookmarks. */
        private const val BOOKMARK_ID_BASE = 900_000

        /** A leading underscore hides the bookmark in Word. The trailing `_` keeps 720 from matching 7200. */
        private val MARK = Regex("_Odr(FirstLine|Hanging)(\\d+)_")

        private fun bookmarkName(twips: Int, index: Int): String {
            return if (twips > 0) "$FIRST_LINE${twips}_$index" else "$HANGING${-twips}_${index}"
        }

        /** Rules for the marks in [outputs], the converted HTML and its scripts; empty when there are none. */
        fun css(outputs: Sequence<String>): String {
            val marks = outputs.flatMap { text -> MARK.findAll(text).map { match -> match.groupValues[1] to match.groupValues[2] } }
                .toSet()
            return marks.joinToString(separator = "") { (kind, twips) ->
                val prefix = if (kind == "FirstLine") FIRST_LINE else HANGING
                val sign = if (kind == "FirstLine") "" else "-"
                // A twip is 1/20 of a point.
                "x-p:has(>a[id^=\"$prefix${twips}_\"]){text-indent:$sign${twips.toInt() / 20.0}pt}"
            }
        }
    }
}

private class IndentResolver(styles: Document?) {
    private val paragraphStyles: Map<String, Element>
    private val defaultStyleId: String?
    private val documentDefault: Element?

    init {
        val root = styles?.documentElement
        val all = root?.children(NS_WORD, "style").orEmpty().filter { style -> style.wordAttribute("type") == "paragraph" }
        paragraphStyles = all.associateBy { style -> style.wordAttribute("styleId").orEmpty() }
        defaultStyleId = all.firstOrNull { style -> style.wordAttribute("default") in setOf("1", "true", "on") }
            ?.wordAttribute("styleId")
        documentDefault = root?.path(NS_WORD, "docDefaults", "pPrDefault", "pPr")
    }

    /**
     * Positive for a first-line indent, negative for a hanging one; `null` for none or a list item.
     * Like Word, the first of the paragraph, its style chain and the document defaults that states it wins.
     */
    fun firstLineTwips(paragraph: Element): Int? {
        val properties = paragraph.child(NS_WORD, "pPr")
        val styleId = properties?.child(NS_WORD, "pStyle")?.wordAttribute("val") ?: defaultStyleId
        val sources = listOfNotNull(properties) + styleChain(styleId) + listOfNotNull(documentDefault)

        // Also with numId 0, which Word reads as "not a list" but the library still lays out as one.
        if (sources.any { source -> source.child(NS_WORD, "numPr") != null }) return null

        return sources.firstNotNullOfOrNull { source -> source.child(NS_WORD, "ind")?.firstLine() }?.takeIf { it != 0 }
    }

    private fun styleChain(styleId: String?): List<Element> {
        val chain = mutableListOf<Element>()
        val visited = mutableSetOf<String>()
        var id = styleId
        while (id != null && visited.add(id)) {
            val style = paragraphStyles[id] ?: break
            style.child(NS_WORD, "pPr")?.let(chain::add)
            id = style.child(NS_WORD, "basedOn")?.wordAttribute("val")
        }
        return chain
    }

    /** `hanging` wins over `firstLine` in the same element, as in Word. */
    private fun Element.firstLine(): Int? {
        wordAttribute("hanging")?.toDoubleOrNull()?.let { return -it.roundToInt() }
        return wordAttribute("firstLine")?.toDoubleOrNull()?.roundToInt()
    }
}

private fun Element.wordAttribute(name: String): String? = getAttributeNS(NS_WORD, name).takeIf { it.isNotEmpty() }
