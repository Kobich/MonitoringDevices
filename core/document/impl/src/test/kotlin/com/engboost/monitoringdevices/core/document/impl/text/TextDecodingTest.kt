package com.engboost.monitoringdevices.core.document.impl.text

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class TextDecodingTest {
    private val russian = "Привет, мир! Это проверка кодировки."
    private val french = "Café crème à la carte, déjà vu."

    @Test
    fun `utf-8 text`() {
        val decoded = TextDecoding.decode(russian.toByteArray(Charsets.UTF_8), isComplete = true)

        assertEquals("UTF-8", decoded.charsetName)
        assertEquals(russian, decoded.text)
    }

    @Test
    fun `utf-8 with bom drops the bom`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "abc".toByteArray()

        assertEquals("abc", TextDecoding.decode(bytes, isComplete = true).text)
    }

    @Test
    fun `utf-16 with and without bom`() {
        val withBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "Hello".toByteArray(Charsets.UTF_16LE)
        val withoutBom = "Hello, world".toByteArray(Charsets.UTF_16LE)

        assertEquals("Hello", TextDecoding.decode(withBom, isComplete = true).text)
        assertEquals("UTF-16LE", TextDecoding.decode(withoutBom, isComplete = true).charsetName)
        assertEquals("Hello, world", TextDecoding.decode(withoutBom, isComplete = true).text)
    }

    @Test
    fun `windows-1251 russian text`() {
        val decoded = TextDecoding.decode(russian.toByteArray(Charset.forName("windows-1251")), isComplete = true)

        assertEquals("windows-1251", decoded.charsetName)
        assertEquals(russian, decoded.text)
    }

    @Test
    fun `latin-1 text`() {
        val decoded = TextDecoding.decode(french.toByteArray(Charsets.ISO_8859_1), isComplete = true)

        assertEquals("ISO-8859-1", decoded.charsetName)
        assertEquals(french, decoded.text)
    }

    @Test
    fun `utf-8 cut in the middle of a character is still utf-8`() {
        val bytes = russian.toByteArray(Charsets.UTF_8)
        val cut = bytes.copyOf(7) // "При" is 6 bytes, the 7th starts "в"

        val decoded = TextDecoding.decode(cut, isComplete = false)

        assertEquals("UTF-8", decoded.charsetName)
        assertEquals("При", decoded.text)
    }

    @Test(expected = TextDecoding.BinaryContentException::class)
    fun `binary content is rejected`() {
        TextDecoding.decode(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x08, 0x00, 0x01, 0x02), isComplete = true)
    }
}
