package com.engboost.monitoringdevices.feature.filemanager.impl.data.source

import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
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

    fun uniqueChild(directory: File, rawName: String): File {
        val name = sanitizeFileName(rawName)
        val candidate = File(directory, name)
        if (!candidate.exists()) return candidate

        val dotIndex = name.lastIndexOf('.')
        val hasExtension = dotIndex > 0
        val baseName = if (hasExtension) name.substring(0, dotIndex) else name
        val extension = if (hasExtension) name.substring(dotIndex) else ""

        return generateSequence(1) { index -> index + 1 }
            .map { index -> File(directory, "$baseName ($index)$extension") }
            .first { file -> !file.exists() }
    }

    companion object {
        private const val FALLBACK_NAME = "unnamed"
        private const val MAX_NAME_LENGTH = 200

        fun sanitizeFileName(rawName: String): String {
            val cleaned = rawName
                .map { char -> if (char == '/' || char == '\\' || char.isISOControl()) '_' else char }
                .joinToString(separator = "")
                .trim()
                .take(MAX_NAME_LENGTH)

            return when (cleaned) {
                "", ".", ".." -> FALLBACK_NAME
                else -> cleaned
            }
        }
    }
}
