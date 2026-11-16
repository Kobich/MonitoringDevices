package com.android.appmanager.feature.filemanager.data.source

import com.android.appmanager.feature.filemanager.domain.model.StoragePath
import java.io.File
import java.io.IOException

/** A path never escapes [rootDirectory] (`..`, symlinks). */
internal class InternalFileSource(
    private val rootDirectory: File
) {
    private val canonicalRoot: File by lazy {
        rootDirectory.mkdirs()
        rootDirectory.canonicalFile
    }

    fun resolve(path: StoragePath): File {
        if (path.isRoot) return canonicalRoot

        val file = File(canonicalRoot, path.value).canonicalFile
        val isInsideRoot = file.path.startsWith(canonicalRoot.path + File.separator)
        if (!isInsideRoot) {
            throw IOException("Path $path is outside of the app storage")
        }
        return file
    }

    /** [rawName] cleaned, or with ` (1)`, ` (2)`… before the extension if taken; always within the name limit. */
    fun uniqueChild(directory: File, rawName: String): File {
        val name = sanitizeFileName(rawName)
        val candidate = File(directory, name)
        if (!candidate.exists()) return candidate

        val (baseName, extension) = splitExtension(name)
        return generateSequence(1) { index -> index + 1 }
            .map { index -> File(directory, fitName(baseName, " ($index)$extension")) }
            .first { file -> !file.exists() }
    }

    companion object {
        private const val FALLBACK_NAME = "unnamed"

        /** The file system limit is in bytes: 255 bytes of UTF-8 are only 127 Cyrillic letters. */
        private const val MAX_NAME_BYTES = 255

        /** A longer "extension" is just a dot in the name and may be cut like the rest of it. */
        private const val MAX_EXTENSION_BYTES = 32

        /** Room for the ` (N)` of [uniqueChild]. */
        private const val SUFFIX_RESERVE_BYTES = 12

        /** A long name is cut in the middle, not at the end: the extension decides how the file opens. */
        fun sanitizeFileName(rawName: String): String {
            val cleaned = rawName
                .map { char -> if (char == '/' || char == '\\' || char.isISOControl()) '_' else char }
                .joinToString(separator = "")
                .trim()

            val name = when (cleaned) {
                "", ".", ".." -> FALLBACK_NAME
                else -> cleaned
            }
            if (name.utf8Size() <= MAX_NAME_BYTES - SUFFIX_RESERVE_BYTES) return name

            val (baseName, extension) = splitExtension(name)
            return fitName(baseName, extension, MAX_NAME_BYTES - SUFFIX_RESERVE_BYTES)
        }

        /** `report.pdf` → (`report`, `.pdf`); a leading dot (`.hidden`) isn't an extension. */
        private fun splitExtension(name: String): Pair<String, String> {
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex <= 0) return name to ""
            val extension = name.substring(dotIndex)
            if (extension.utf8Size() > MAX_EXTENSION_BYTES) return name to ""
            return name.substring(0, dotIndex) to extension
        }

        /** [baseName] cut to whole characters so that it and [ending] fit in [maxBytes]. */
        private fun fitName(baseName: String, ending: String, maxBytes: Int = MAX_NAME_BYTES): String {
            val budget = maxBytes - ending.utf8Size()
            var bytes = 0
            var end = 0
            while (end < baseName.length) {
                val codePoint = baseName.codePointAt(end)
                val size = String(Character.toChars(codePoint)).utf8Size()
                if (bytes + size > budget) break
                bytes += size
                end += Character.charCount(codePoint)
            }
            val cut = baseName.substring(0, end).trimEnd()
            return cut.ifEmpty { FALLBACK_NAME } + ending
        }

        private fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size
    }
}
