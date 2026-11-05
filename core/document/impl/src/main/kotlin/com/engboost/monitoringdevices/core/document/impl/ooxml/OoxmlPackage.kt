package com.engboost.monitoringdevices.core.document.impl.ooxml

import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_CONTENT_TYPES
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_PACKAGE_RELATIONSHIPS
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.REL_IMAGE
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.REL_OFFICE_DOCUMENT
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.children
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Parsed parts are cached, so edits accumulate; [writeTo] writes a copy with the changes. */
internal class OoxmlPackage(private val zip: ZipFile) {
    private val parts = mutableMapOf<String, Document>()
    private val modifiedParts = linkedSetOf<String>()
    private val relationships = mutableMapOf<String, Relationships>()
    private val addedFiles = linkedMapOf<String, ByteArray>()

    val hasChanges: Boolean
        get() = addedFiles.isNotEmpty() || modifiedParts.isNotEmpty()

    fun mainPartPath(fallback: String): String {
        return relationships("").firstTargetOfType(REL_OFFICE_DOCUMENT) ?: fallback
    }

    fun part(path: String): Document = partOrNull(path) ?: throw IOException("$path is missing")

    fun partOrNull(path: String): Document? {
        parts[path]?.let { return it }
        val entry = zip.getEntry(path) ?: return null
        if (entry.size > MAX_XML_PART_BYTES) throw IOException("$path is too large")
        return zip.getInputStream(entry).use(OoxmlXml::parse).also { document -> parts[path] = document }
    }

    fun markModified(path: String) {
        modifiedParts += path
    }

    /** Relationships of [partPath]; `""` for the package itself. */
    fun relationships(partPath: String): Relationships {
        return relationships.getOrPut(partPath) {
            val path = if (partPath.isEmpty()) "_rels/.rels" else OoxmlXml.relationshipsPath(partPath)
            val document = partOrNull(path) ?: OoxmlXml.parse(EMPTY_RELATIONSHIPS.byteInputStream())
            parts[path] = document
            Relationships(partPath, path, document)
        }
    }

    fun themeOf(partPath: String): Document? {
        return relationships(partPath).firstTargetOfType(OoxmlXml.REL_THEME)?.let(::partOrNull)
    }

    /** @return id of the new image relationship of [partPath]. */
    fun addImage(partPath: String, png: ByteArray): String {
        val relationships = relationships(partPath)
        val mediaFolder = if (partPath.contains('/')) "${partPath.substringBefore('/')}/media" else "media"
        val mediaPath = generateSequence(1) { it + 1 }
            .map { index -> "$mediaFolder/chart-image-$index.png" }
            .first { path -> zip.getEntry(path) == null && path !in addedFiles }
        addedFiles[mediaPath] = png
        return relationships.add(REL_IMAGE, OoxmlXml.relativeTarget(partPath, mediaPath))
    }

    fun writeTo(output: File) {
        if (addedFiles.isNotEmpty()) {
            part(CONTENT_TYPES_PATH).ensureDefault(extension = "png", contentType = "image/png")
            markModified(CONTENT_TYPES_PATH)
        }
        relationships.values.filter { it.isModified }.forEach { markModified(it.path) }

        val serialized = modifiedParts.associateWith { path -> OoxmlXml.serialize(part(path)) }
        val written = mutableSetOf<String>()
        ZipOutputStream(output.outputStream().buffered()).use { out ->
            zip.entries().asSequence().forEach { entry ->
                out.putNextEntry(ZipEntry(entry.name))
                val replacement = serialized[entry.name]
                if (replacement != null) out.write(replacement) else zip.getInputStream(entry).use { it.copyTo(out) }
                out.closeEntry()
                written += entry.name
            }
            (serialized.filterKeys { it !in written } + addedFiles).forEach { (path, bytes) ->
                out.putNextEntry(ZipEntry(path))
                out.write(bytes)
                out.closeEntry()
            }
        }
    }

    class Relationships(
        private val partPath: String,
        val path: String,
        private val document: Document
    ) {
        var isModified = false
            private set

        private val entries get() = document.documentElement.children(NS_PACKAGE_RELATIONSHIPS, "Relationship")

        fun target(id: String?): String? {
            id ?: return null
            val relationship = entries.firstOrNull { it.getAttribute("Id") == id } ?: return null
            if (relationship.getAttribute("TargetMode") == "External") return null
            return OoxmlXml.resolveTarget(partPath, relationship.getAttribute("Target"))
        }

        fun targetsOfType(type: String): List<String> {
            return entries.filter { it.getAttribute("Type") == type && it.getAttribute("TargetMode") != "External" }
                .map { OoxmlXml.resolveTarget(partPath, it.getAttribute("Target")) }
        }

        fun firstTargetOfType(type: String): String? = targetsOfType(type).firstOrNull()

        fun add(type: String, target: String): String {
            val usedIds = entries.map { it.getAttribute("Id") }.toSet()
            val id = generateSequence(1) { it + 1 }.map { "rIdChartImage$it" }.first { it !in usedIds }
            document.documentElement.appendChild(
                document.createElementNS(NS_PACKAGE_RELATIONSHIPS, "Relationship").apply {
                    setAttribute("Id", id)
                    setAttribute("Type", type)
                    setAttribute("Target", target)
                }
            )
            isModified = true
            return id
        }
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

    companion object {
        const val CONTENT_TYPES_PATH = "[Content_Types].xml"
        private const val MAX_XML_PART_BYTES = 64L * 1024 * 1024
        private const val EMPTY_RELATIONSHIPS =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>"""
    }
}
