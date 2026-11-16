package com.android.core.document.impl.office

import android.util.Log
import com.android.core.document.impl.ooxml.OoxmlPackage
import com.android.core.document.impl.ooxml.OoxmlXml.NS_WORD
import com.android.core.document.impl.ooxml.OoxmlXml.REL_STYLES
import com.android.core.document.impl.ooxml.OoxmlXml.child
import com.android.core.document.impl.ooxml.OoxmlXml.descendants
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile
import kotlin.math.roundToInt

private const val TAG = "DocxFirstLineIndents"

/**
 * OpenDocument.core drops first-line and hanging indents: every line of a paragraph starts at its left
 * indent. Each such paragraph gets a hidden bookmark that names the indent (see [WordBookmarks]), and
 * [css] turns it into `text-indent`. List items are skipped: the library lays them out on its own.
 */
internal class DocxFirstLineIndents : DocumentPatch {

    override fun apply(input: File, output: File): Boolean {
        ZipFile(input).use { zip ->
            val ooxml = OoxmlPackage(zip)
            val documentPath = ooxml.mainPartPath(fallback = "word/document.xml")
            val document = ooxml.part(documentPath)
            val stylesPart = ooxml.relationships(documentPath).firstTargetOfType(REL_STYLES)?.let(ooxml::partOrNull)
            val styles = WordParagraphStyles(stylesPart)
            val bookmarks = WordBookmarks(document)

            var marked = 0
            document.documentElement.descendants(NS_WORD, "p").forEach { paragraph ->
                val twips = firstLineTwips(styles.sources(paragraph)) ?: return@forEach
                bookmarks.insertAtStart(paragraph, bookmarkName(twips, marked))
                marked++
            }

            if (marked == 0) return false
            ooxml.markModified(documentPath)
            ooxml.writeTo(output)
            Log.d(TAG, "Marked $marked paragraph(s)")
            return true
        }
    }

    /** Rules for the marks in [outputs], the converted HTML and its scripts; empty when there are none. */
    override fun css(outputs: Sequence<String>): String {
        val marks = outputs
            .flatMap { text -> MARK.findAll(text).map { match -> match.groupValues[1] to match.groupValues[2] } }
            .toSet()
        return marks.joinToString(separator = "") { (kind, twips) ->
            val prefix = if (kind == "FirstLine") FIRST_LINE else HANGING
            val sign = if (kind == "FirstLine") "" else "-"
            // A twip is 1/20 of a point.
            "x-p:has(>a[id^=\"$prefix${twips}_\"]){text-indent:$sign${twips.toLong() / 20.0}pt}"
        }
    }

    /**
     * Positive for a first-line indent, negative for a hanging one; `null` for none or a list item.
     * Like Word, the first of [sources] that states it wins.
     */
    private fun firstLineTwips(sources: List<Element>): Int? {
        // Also with numId 0, which Word reads as "not a list" but the library still lays out as one.
        if (sources.any { source -> source.child(NS_WORD, "numPr") != null }) return null
        return sources.firstNotNullOfOrNull { source -> source.child(NS_WORD, "ind")?.firstLine() }?.takeIf { it != 0 }
    }

    /** `hanging` wins over `firstLine` in the same element, as in Word. */
    private fun Element.firstLine(): Int? {
        wordAttribute("hanging")?.toDoubleOrNull()?.let { return -it.roundToInt() }
        return wordAttribute("firstLine")?.toDoubleOrNull()?.roundToInt()
    }

    private companion object {
        const val FIRST_LINE = "_OdrFirstLine"
        const val HANGING = "_OdrHanging"

        /** A leading underscore hides the bookmark in Word. The trailing `_` keeps 720 from matching 7200. */
        val MARK = Regex("_Odr(FirstLine|Hanging)(\\d{1,10})_")

        fun bookmarkName(twips: Int, index: Int): String {
            return if (twips > 0) "$FIRST_LINE${twips}_$index" else "$HANGING${-twips}_$index"
        }
    }
}
