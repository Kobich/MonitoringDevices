package com.android.appmanager.feature.filemanager

import com.android.appmanager.feature.filemanager.domain.model.StoragePath
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Files of a storage that open only with a password, as the administrator set them. The files themselves aren't
 * encrypted: the file manager asks for the password before it opens, exports or deletes them.
 *
 * @throws IllegalArgumentException if a path is invalid or listed in two entries.
 */
class FilePasswords(val entries: List<Entry>) {

    init {
        val paths = entries.flatMap { entry -> entry.paths }
        val repeated = paths.groupingBy { it }.eachCount().filterValues { count -> count > 1 }.keys
        require(repeated.isEmpty()) { "Paths with more than one password: $repeated" }
    }

    /**
     * One password for [paths]. A path is relative to the storage root, segments separated by `/` (`\` is read as
     * `/` too): `report.pdf` is a file in the root, `Reports/2026/q1.pdf` one in a folder. Paths are case-sensitive.
     *
     * @throws IllegalArgumentException if [paths] is empty or a path is empty or has a `..` segment.
     */
    class Entry(paths: List<String>, val password: PasswordHash) {
        /** Normalized: no leading, trailing or repeated separators, no `.` segments. */
        val paths: List<String> = paths.map(::normalize)

        internal val storagePaths: List<StoragePath> = this.paths.map(::StoragePath)

        init {
            require(paths.isNotEmpty()) { "An entry without paths" }
        }

        override fun equals(other: Any?): Boolean =
            other is Entry && other.paths == paths && other.password == password

        override fun hashCode(): Int = 31 * paths.hashCode() + password.hashCode()

        private fun normalize(path: String): String {
            val segments = path.split('/', '\\').filter { segment -> segment.isNotEmpty() && segment != "." }
            require(segments.isNotEmpty()) { "Empty path: \"$path\"" }
            require(segments.none { segment -> segment == ".." }) { "Path with \"..\": \"$path\"" }
            return segments.joinToString(separator = "/")
        }
    }

    /** Entries protecting [path] itself or, for a folder, any file under it. */
    internal fun entriesCovering(path: StoragePath): List<Entry> =
        entries.filter { entry -> entry.storagePaths.any { locked -> locked.isInside(path) } }

    /** The entry protecting the file at [path], if any. */
    internal fun entryOf(path: StoragePath): Entry? = entries.firstOrNull { entry -> path in entry.storagePaths }

    override fun equals(other: Any?): Boolean = other is FilePasswords && other.entries == entries

    override fun hashCode(): Int = entries.hashCode()
}

/**
 * A password stored as a PBKDF2 hash: `hash = PBKDF2(algorithm, UTF-8 password, salt, iterations)`, as long as
 * [hash]. The password itself is never stored.
 */
class PasswordHash(
    val algorithm: Algorithm,
    val iterations: Int,
    salt: ByteArray,
    hash: ByteArray
) {
    private val salt: ByteArray = salt.copyOf()
    private val hash: ByteArray = hash.copyOf()

    init {
        require(iterations > 0) { "iterations must be positive" }
        require(this.salt.isNotEmpty()) { "Empty salt" }
        require(this.hash.isNotEmpty()) { "Empty hash" }
    }

    enum class Algorithm(val jcaName: String) {
        PBKDF2_HMAC_SHA1("PBKDF2WithHmacSHA1"),

        /** Android 8.0 (API 26) and newer. */
        PBKDF2_HMAC_SHA256("PBKDF2WithHmacSHA256");

        companion object {
            /** By [jcaName], as a config would name it; `null` if unknown. */
            fun fromJcaName(name: String): Algorithm? = entries.firstOrNull { algorithm -> algorithm.jcaName == name }
        }
    }

    /**
     * Slow on purpose (that's what the iterations are for): call off the main thread.
     *
     * @throws java.security.NoSuchAlgorithmException if the device doesn't support [algorithm].
     */
    internal fun matches(password: CharArray): Boolean {
        val spec = PBEKeySpec(password, salt, iterations, hash.size * Byte.SIZE_BITS)
        try {
            val actual = SecretKeyFactory.getInstance(algorithm.jcaName).generateSecret(spec).encoded
            return MessageDigest.isEqual(actual, hash)
        } finally {
            spec.clearPassword()
        }
    }

    override fun equals(other: Any?): Boolean =
        other is PasswordHash && other.algorithm == algorithm && other.iterations == iterations &&
            other.salt.contentEquals(salt) && other.hash.contentEquals(hash)

    override fun hashCode(): Int = 31 * (31 * algorithm.hashCode() + iterations) + hash.contentHashCode()

    /** Never shows the hash: it could end up in logs. */
    override fun toString(): String = "PasswordHash($algorithm, $iterations iterations)"
}
