package com.android.core.document.impl.ooxml

import org.w3c.dom.Document
import java.io.File
import java.util.zip.ZipFile

/** The main part of an opened Word file (`word/document.xml`), as a DOCX patch changes it. */
internal class WordMainPart(val ooxml: OoxmlPackage, val path: String) {
    val document: Document = ooxml.part(path)
    val relationships: OoxmlPackage.Relationships by lazy { ooxml.relationships(path) }
}

/**
 * Opens the main part of the Word file [input] for [edit], which returns how many places it changed. Only then is the
 * changed file written to [output]; the result says whether it was.
 */
internal inline fun editWordMainPart(input: File, output: File, edit: (WordMainPart) -> Int): Boolean {
    ZipFile(input).use { zip ->
        val ooxml = OoxmlPackage(zip)
        val part = WordMainPart(ooxml, ooxml.mainPartPath(fallback = "word/document.xml"))
        if (edit(part) == 0) return false
        ooxml.markModified(part.path)
        ooxml.writeTo(output)
        return true
    }
}
