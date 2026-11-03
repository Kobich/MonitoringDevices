package com.engboost.monitoringdevices.core.document.impl.ooxml

import org.junit.Assert.assertEquals
import org.junit.Test

class OoxmlXmlTest {
    @Test
    fun `targets resolve against the folder of the part`() {
        assertEquals("word/charts/chart1.xml", OoxmlXml.resolveTarget("word/document.xml", "charts/chart1.xml"))
        assertEquals("ppt/media/image1.png", OoxmlXml.resolveTarget("ppt/slides/slide1.xml", "../media/image1.png"))
        assertEquals("word/document.xml", OoxmlXml.resolveTarget("", "word/document.xml"))
        assertEquals("absolute target", "ppt/theme/theme1.xml", OoxmlXml.resolveTarget("ppt/slides/slide1.xml", "/ppt/theme/theme1.xml"))
    }

    @Test
    fun `relative target is the inverse of resolve`() {
        listOf(
            "word/document.xml" to "word/media/chart-image-1.png",
            "ppt/slides/slide1.xml" to "ppt/media/chart-image-1.png",
            "document.xml" to "media/chart-image-1.png"
        ).forEach { (part, target) ->
            assertEquals(target, OoxmlXml.resolveTarget(part, OoxmlXml.relativeTarget(part, target)))
        }
        assertEquals("../media/x.png", OoxmlXml.relativeTarget("ppt/slides/slide1.xml", "ppt/media/x.png"))
    }

    @Test
    fun `relationships part sits in the _rels folder next to the part`() {
        assertEquals("word/_rels/document.xml.rels", OoxmlXml.relationshipsPath("word/document.xml"))
        assertEquals("_rels/document.xml.rels", OoxmlXml.relationshipsPath("document.xml"))
    }
}
