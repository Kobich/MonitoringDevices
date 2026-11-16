package com.android.core.document.impl.office

import com.android.core.document.impl.ooxml.OoxmlXml.NS_WORD
import com.android.core.document.impl.ooxml.OoxmlXml.NS_XMLNS
import com.android.core.document.impl.ooxml.OoxmlXml.child
import com.android.core.document.impl.ooxml.OoxmlXml.children
import com.android.core.document.impl.ooxml.OoxmlXml.descendants
import com.android.core.document.impl.ooxml.OoxmlXml.path
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Paragraph properties of a DOCX the way Word resolves them: [sources] lists the paragraph's own `pPr`,
 * then those of its style and the styles it is based on (or of the default paragraph style), then the
 * document defaults.
 */
internal class WordParagraphStyles(styles: Document?) {
    private val paragraphStyles: Map<String, Element>
    private val defaultStyleId: String?
    private val documentDefault: Element?

    init {
        val root = styles?.documentElement
        // A style without a type is a paragraph style.
        val all = root?.children(NS_WORD, "style").orEmpty()
            .filter { style -> (style.wordAttribute("type") ?: "paragraph") == "paragraph" }
        paragraphStyles = all.associateBy { style -> style.wordAttribute("styleId").orEmpty() }
        defaultStyleId = all.firstOrNull { style -> style.wordAttribute("default") in setOf("1", "true", "on") }
            ?.wordAttribute("styleId")
        documentDefault = root?.path(NS_WORD, "docDefaults", "pPrDefault", "pPr")
    }

    /** The most specific first. */
    fun sources(paragraph: Element): List<Element> {
        val properties = paragraph.child(NS_WORD, "pPr")
        val styleId = properties?.child(NS_WORD, "pStyle")?.wordAttribute("val") ?: defaultStyleId
        return listOfNotNull(properties) + styleChain(styleId) + listOfNotNull(documentDefault)
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
}

/**
 * Inserts hidden bookmarks at the start of paragraphs. OpenDocument.core keeps a bookmark as `<a id>` at
 * the start of the paragraph, which lets a patch pass a value from the DOCX to the converted HTML. Ids
 * continue after the largest one in [document], so patches that run one after another don't clash.
 */
internal class WordBookmarks(private val document: Document) {
    private var nextId = document.documentElement.descendants(NS_WORD, "bookmarkStart")
        .mapNotNull { bookmark -> bookmark.wordAttribute("id")?.toIntOrNull() }
        .maxOrNull()?.plus(1) ?: 0

    fun insertAtStart(paragraph: Element, name: String) {
        val id = (nextId++).toString()
        // The prefix is declared here: the serializer doesn't, and a document may bind the namespace to another one.
        val start = document.createElementNS(NS_WORD, "w:bookmarkStart").apply {
            setAttributeNS(NS_XMLNS, "xmlns:w", NS_WORD)
            setAttributeNS(NS_WORD, "w:id", id)
            setAttributeNS(NS_WORD, "w:name", name)
        }
        val end = document.createElementNS(NS_WORD, "w:bookmarkEnd").apply {
            setAttributeNS(NS_XMLNS, "xmlns:w", NS_WORD)
            setAttributeNS(NS_WORD, "w:id", id)
        }
        val properties = paragraph.child(NS_WORD, "pPr")
        val before = if (properties != null) properties.nextSibling else paragraph.firstChild
        paragraph.insertBefore(start, before)
        paragraph.insertBefore(end, before)
    }
}

internal fun Element.wordAttribute(name: String): String? = getAttributeNS(NS_WORD, name).takeIf { it.isNotEmpty() }
