package com.android.appmanager.feature.filemanager

import com.android.appmanager.feature.filemanager.PasswordHash.Algorithm
import com.android.appmanager.feature.filemanager.domain.model.StoragePath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FilePasswordsTest {

    @Test
    fun `paths are normalized`() {
        val entry = FilePasswords.Entry(listOf("/a//b/./c.pdf/", "Docs\\q1.xlsx", "report.pdf"), sha256)
        assertEquals(listOf("a/b/c.pdf", "Docs/q1.xlsx", "report.pdf"), entry.paths)
    }

    @Test
    fun `invalid paths are rejected`() {
        listOf("", "/", "./", "a/../b", "..").forEach { path ->
            assertThrows(path, IllegalArgumentException::class.java) { FilePasswords.Entry(listOf(path), sha256) }
        }
        assertThrows(IllegalArgumentException::class.java) { FilePasswords.Entry(emptyList(), sha256) }
    }

    @Test
    fun `a path can't have two passwords`() {
        assertThrows(IllegalArgumentException::class.java) {
            FilePasswords(listOf(FilePasswords.Entry(listOf("a.pdf"), sha256), FilePasswords.Entry(listOf("/a.pdf"), sha1)))
        }
    }

    @Test
    fun `a file is protected by its own entry only`() {
        val first = FilePasswords.Entry(listOf("Docs/a.pdf", "b.pdf"), sha256)
        val second = FilePasswords.Entry(listOf("Docs/Old/c.pdf"), sha1)
        val passwords = FilePasswords(listOf(first, second))

        assertEquals(first, passwords.entryOf(StoragePath("Docs/a.pdf")))
        assertEquals(first, passwords.entryOf(StoragePath("b.pdf")))
        assertEquals(second, passwords.entryOf(StoragePath("Docs/Old/c.pdf")))
        assertNull(passwords.entryOf(StoragePath("a.pdf")))
        assertNull(passwords.entryOf(StoragePath("Docs/A.pdf")))
    }

    @Test
    fun `a folder is covered by the files inside it`() {
        val first = FilePasswords.Entry(listOf("Docs/a.pdf"), sha256)
        val second = FilePasswords.Entry(listOf("Docs/Old/c.pdf"), sha1)
        val passwords = FilePasswords(listOf(first, second))

        assertEquals(listOf(first, second), passwords.entriesCovering(StoragePath.ROOT))
        assertEquals(listOf(first, second), passwords.entriesCovering(StoragePath("Docs")))
        assertEquals(listOf(second), passwords.entriesCovering(StoragePath("Docs/Old")))
        assertEquals(listOf(first), passwords.entriesCovering(StoragePath("Docs/a.pdf")))
        assertEquals(emptyList<FilePasswords.Entry>(), passwords.entriesCovering(StoragePath("Doc")))
        assertEquals(emptyList<FilePasswords.Entry>(), passwords.entriesCovering(StoragePath("Docs/Old/c")))
    }

    @Test
    fun `hashes made by the admin tool match`() {
        assertTrue(sha256.matches("пароль-1".toCharArray()))
        assertTrue(sha1.matches("пароль-1".toCharArray()))
        assertFalse(sha256.matches("пароль-2".toCharArray()))
        assertFalse(sha256.matches(CharArray(0)))
    }

    @Test
    fun `equal content is equal`() {
        val copy = PasswordHash(Algorithm.PBKDF2_HMAC_SHA256, 1000, SALT.copyOf(), hex(SHA256_HASH))
        assertEquals(sha256, copy)
        assertEquals(
            FilePasswords(listOf(FilePasswords.Entry(listOf("a.pdf"), sha256))),
            FilePasswords(listOf(FilePasswords.Entry(listOf("/a.pdf"), copy)))
        )
        assertEquals(Algorithm.PBKDF2_HMAC_SHA1, Algorithm.fromJcaName("PBKDF2WithHmacSHA1"))
        assertNull(Algorithm.fromJcaName("MD5"))
    }

    private companion object {
        val SALT = "0123456789abcdef".toByteArray()

        // hashlib.pbkdf2_hmac(digest, "пароль-1".encode(), b"0123456789abcdef", 1000, 32), as tools/password_hash.py does
        const val SHA256_HASH = "10c533ae5ef99f829df96c3eed499b0fb5c367f518ab07582463e713460f24ca"
        const val SHA1_HASH = "0cea0c023af41bd028e48a11f3e7e1100f73ccd42e1eac1e57720e161279cd8d"

        val sha256 = PasswordHash(Algorithm.PBKDF2_HMAC_SHA256, 1000, SALT, hex(SHA256_HASH))
        val sha1 = PasswordHash(Algorithm.PBKDF2_HMAC_SHA1, 1000, SALT, hex(SHA1_HASH))

        fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
