package com.engboost.monitoringdevices.core.document.impl.ooxml

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Namespaces and DOM helpers for Office Open XML parts. */
internal object OoxmlXml {
    const val NS_CHART = "http://schemas.openxmlformats.org/drawingml/2006/chart"
    const val NS_DRAWING = "http://schemas.openxmlformats.org/drawingml/2006/main"
    const val NS_PICTURE = "http://schemas.openxmlformats.org/drawingml/2006/picture"
    const val NS_WORD_DRAWING = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    const val NS_RELATIONSHIPS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    const val NS_PACKAGE_RELATIONSHIPS = "http://schemas.openxmlformats.org/package/2006/relationships"
    const val NS_CONTENT_TYPES = "http://schemas.openxmlformats.org/package/2006/content-types"
    const val NS_XMLNS = "http://www.w3.org/2000/xmlns/"

    const val REL_OFFICE_DOCUMENT = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
    const val REL_IMAGE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image"
    const val REL_THEME = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme"

    fun parse(input: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            // Untrusted input: no DTDs, no external entities. Not every platform parser
            // knows these features, Android's rejects DOCTYPE-based tricks on its own.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        return factory.newDocumentBuilder().parse(input)
    }

    fun serialize(document: Document): ByteArray {
        val output = ByteArrayOutputStream()
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "no")
        }
        transformer.transform(DOMSource(document), StreamResult(output))
        return output.toByteArray()
    }

    fun Element.children(namespace: String, localName: String): List<Element> {
        return childElements().filter { child -> child.namespaceURI == namespace && child.localName == localName }
    }

    fun Element.child(namespace: String, localName: String): Element? = children(namespace, localName).firstOrNull()

    fun Element.childElements(): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).mapNotNull { index -> nodes.item(index) as? Element }
    }

    /** Descendant by a path of local names in one namespace, e.g. `tx/strRef/strCache`. */
    fun Element.path(namespace: String, vararg localNames: String): Element? {
        return localNames.fold(this as Element?) { element, name -> element?.child(namespace, name) }
    }

    fun Element.descendants(namespace: String, localName: String): List<Element> {
        val nodes = getElementsByTagNameNS(namespace, localName)
        return (0 until nodes.length).map { index -> nodes.item(index) as Element }
    }

    fun Element.valAttribute(): String? = getAttribute("val").takeIf { it.isNotEmpty() }

    fun Node.ancestor(namespace: String, vararg localNames: String): Element? {
        var node: Node? = parentNode
        while (node != null) {
            if (node is Element && node.namespaceURI == namespace && node.localName in localNames) return node
            node = node.parentNode
        }
        return null
    }

    /** Resolves a relationship target against the folder of the part that owns it. */
    fun resolveTarget(partPath: String, target: String): String {
        if (target.startsWith("/")) return target.removePrefix("/")
        val segments = partPath.substringBeforeLast('/', missingDelimiterValue = "")
            .split('/')
            .filter { it.isNotEmpty() }
            .toMutableList()
        target.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments += segment
            }
        }
        return segments.joinToString("/")
    }

    /** Inverse of [resolveTarget]: [targetPath] relative to the folder of [partPath]. */
    fun relativeTarget(partPath: String, targetPath: String): String {
        val from = partPath.substringBeforeLast('/', missingDelimiterValue = "").split('/').filter { it.isNotEmpty() }
        val to = targetPath.split('/')
        val common = from.zip(to).takeWhile { (a, b) -> a == b }.size
        return (List(from.size - common) { ".." } + to.drop(common)).joinToString("/")
    }

    /** `word/document.xml` -> `word/_rels/document.xml.rels`. */
    fun relationshipsPath(partPath: String): String {
        val folder = partPath.substringBeforeLast('/', missingDelimiterValue = "")
        val name = partPath.substringAfterLast('/')
        return if (folder.isEmpty()) "_rels/$name.rels" else "$folder/_rels/$name.rels"
    }
}
