package com.android.core.document.impl.text

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** A dedicated detector (ICU `CharsetDetector`, juniversalchardet) can replace the heuristics later. */
internal object TextDecoding {

    class Decoded(val text: String, val charsetName: String)

    class BinaryContentException : Exception("The file looks binary, not text")

    /**
     * @param isComplete `false` if [bytes] is a prefix of a longer file: an incomplete
     * multibyte character at the end is then ignored instead of breaking UTF-8 detection.
     */
    fun decode(bytes: ByteArray, isComplete: Boolean): Decoded {
        bomCharset(bytes)?.let { (charset, bomSize) ->
            return Decoded(lenient(bytes.copyOfRange(bomSize, bytes.size), charset), charset.name())
        }
        utf16WithoutBom(bytes)?.let { charset ->
            return Decoded(lenient(bytes, charset), charset.name())
        }
        if (looksBinary(bytes)) throw BinaryContentException()

        strictUtf8(bytes, isComplete)?.let { text -> return Decoded(text, Charsets.UTF_8.name()) }

        val fallback = if (looksLikeCyrillicSingleByte(bytes)) WINDOWS_1251 else Charsets.ISO_8859_1
        return Decoded(lenient(bytes, fallback), fallback.name())
    }

    private fun bomCharset(bytes: ByteArray): Pair<Charset, Int>? {
        fun startsWith(vararg prefix: Int) =
            bytes.size >= prefix.size && prefix.indices.all { index -> bytes[index].toInt() and 0xFF == prefix[index] }

        return when {
            startsWith(0xEF, 0xBB, 0xBF) -> Charsets.UTF_8 to 3
            startsWith(0xFF, 0xFE) -> Charsets.UTF_16LE to 2
            startsWith(0xFE, 0xFF) -> Charsets.UTF_16BE to 2
            else -> null
        }
    }

    /** ASCII-heavy UTF-16 has a zero in every other byte. */
    private fun utf16WithoutBom(bytes: ByteArray): Charset? {
        val sample = bytes.take(SAMPLE_SIZE)
        if (sample.size < 4) return null
        val evenZeros = sample.indices.count { it % 2 == 0 && sample[it] == 0.toByte() }
        val oddZeros = sample.indices.count { it % 2 == 1 && sample[it] == 0.toByte() }
        val half = sample.size / 2
        return when {
            oddZeros > half * 0.4 && evenZeros < half * 0.05 -> Charsets.UTF_16LE
            evenZeros > half * 0.4 && oddZeros < half * 0.05 -> Charsets.UTF_16BE
            else -> null
        }
    }

    private fun looksBinary(bytes: ByteArray): Boolean {
        val sample = bytes.take(SAMPLE_SIZE)
        val controls = sample.count { byte ->
            val value = byte.toInt() and 0xFF
            value == 0 || (value < 0x20 && value !in ALLOWED_CONTROLS)
        }
        return sample.any { it == 0.toByte() } || controls > sample.size / 20
    }

    private fun strictUtf8(bytes: ByteArray, isComplete: Boolean): String? {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val input = ByteBuffer.wrap(bytes)
        val output = CharBuffer.allocate(bytes.size)
        val result = decoder.decode(input, output, isComplete)
        if (result.isError) return null
        // A cut file may end in the middle of a character: up to 3 bytes stay undecoded.
        if (isComplete) {
            if (decoder.flush(output).isError) return null
        } else if (input.remaining() > 3) {
            return null
        }
        output.flip()
        return output.toString()
    }

    /**
     * In Cyrillic Windows-1251 text letters (0xC0-0xFF) form whole words, so high bytes
     * mostly stand next to each other; in Latin-1 text accented letters are isolated.
     */
    private fun looksLikeCyrillicSingleByte(bytes: ByteArray): Boolean {
        var high = 0
        var adjacent = 0
        for (index in bytes.indices) {
            if (!bytes[index].isHigh()) continue
            high++
            val previousHigh = index > 0 && bytes[index - 1].isHigh()
            val nextHigh = index < bytes.lastIndex && bytes[index + 1].isHigh()
            if (previousHigh || nextHigh) adjacent++
        }
        return high > 0 && adjacent * 2 > high
    }

    private fun Byte.isHigh(): Boolean = toInt() and 0xFF >= 0x80

    private fun lenient(bytes: ByteArray, charset: Charset): String {
        return charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }

    private const val SAMPLE_SIZE = 8 * 1024
    private val ALLOWED_CONTROLS = setOf(0x09, 0x0A, 0x0C, 0x0D, 0x1B)
    private val WINDOWS_1251: Charset = Charset.forName("windows-1251")
}
