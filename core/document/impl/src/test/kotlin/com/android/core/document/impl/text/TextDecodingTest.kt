package com.android.core.document.impl.text

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class TextDecodingTest {
    private val russian = "Привет, мир! Проверка определения кодировки."

    @Test
    fun `utf-8 without bom`() {
        val decoded = TextDecoding.decode(russian.toByteArray(Charsets.UTF_8), isComplete = true)

        assertEquals("UTF-8", decoded.charsetName)
        assertEquals(russian, decoded.text)
    }

    @Test
    fun `the utf-8 bom is dropped from the text`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + russian.toByteArray(Charsets.UTF_8)

        val decoded = TextDecoding.decode(bytes, isComplete = true)

        assertEquals("UTF-8", decoded.charsetName)
        assertEquals(russian, decoded.text)
    }

    @Test
    fun `utf-16 with a bom`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + russian.toByteArray(Charsets.UTF_16LE)

        val decoded = TextDecoding.decode(bytes, isComplete = true)

        assertEquals("UTF-16LE", decoded.charsetName)
        assertEquals(russian, decoded.text)
    }

    @Test
    fun `ascii utf-16 without a bom is recognized by its zero bytes`() {
        val text = "Plain ASCII text saved as UTF-16"

        val decoded = TextDecoding.decode(text.toByteArray(Charsets.UTF_16LE), isComplete = true)

        assertEquals("UTF-16LE", decoded.charsetName)
        assertEquals(text, decoded.text)
    }

    @Test
    fun `cyrillic single-byte text is windows-1251`() {
        val decoded = TextDecoding.decode(russian.toByteArray(Charset.forName("windows-1251")), isComplete = true)

        assertEquals("windows-1251", decoded.charsetName)
        assertEquals(russian, decoded.text)
    }

    @Test
    fun `isolated accented letters are latin-1`() {
        val text = "Café, déjà vu, naïve"

        val decoded = TextDecoding.decode(text.toByteArray(Charsets.ISO_8859_1), isComplete = true)

        assertEquals("ISO-8859-1", decoded.charsetName)
        assertEquals(text, decoded.text)
    }

    @Test
    fun `a prefix cut inside a character is still utf-8`() {
        val bytes = "Привет".toByteArray(Charsets.UTF_8)

        val decoded = TextDecoding.decode(bytes.copyOf(bytes.size - 1), isComplete = false)

        assertEquals("UTF-8", decoded.charsetName)
        assertEquals("Приве", decoded.text)
    }

    @Test(expected = TextDecoding.BinaryContentException::class)
    fun `bytes with zeros are binary`() {
        TextDecoding.decode(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x08, 0x00, 0x7F, 0x01), isComplete = true)
    }
}
