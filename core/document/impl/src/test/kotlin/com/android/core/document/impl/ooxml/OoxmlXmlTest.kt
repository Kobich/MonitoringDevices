package com.android.core.document.impl.ooxml

import org.junit.Assert.assertEquals
import org.junit.Test

class OoxmlXmlTest {
    @Test
    fun `targets resolve against the folder of the owning part`() {
        assertEquals("word/media/image1.png", OoxmlXml.resolveTarget("word/document.xml", "media/image1.png"))
        assertEquals("ppt/media/image1.png", OoxmlXml.resolveTarget("ppt/slides/slide1.xml", "../media/image1.png"))
        assertEquals("word/styles.xml", OoxmlXml.resolveTarget("word/document.xml", "/word/styles.xml"))
        assertEquals("word/document.xml", OoxmlXml.resolveTarget("", "word/document.xml"))
    }

    @Test
    fun `relative target is the inverse of resolve`() {
        listOf(
            "word/document.xml" to "word/media/chart-image-1.png",
            "ppt/slides/slide1.xml" to "ppt/media/chart-image-1.png"
        ).forEach { (part, target) ->
            assertEquals(target, OoxmlXml.resolveTarget(part, OoxmlXml.relativeTarget(part, target)))
        }
        assertEquals("../media/a.png", OoxmlXml.relativeTarget("ppt/slides/slide1.xml", "ppt/media/a.png"))
    }

    @Test
    fun `relationships live next to their part`() {
        assertEquals("word/_rels/document.xml.rels", OoxmlXml.relationshipsPath("word/document.xml"))
    }
}
