package com.android.core.document.impl.cache

import android.util.Log
import com.android.core.document.api.document.HtmlPage
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "RenderCache"

/**
 * One folder per (file version, variant), published atomically by rename.
 * Above [maxBytes] the least recently used entries are removed after each new one.
 */
internal class RenderCache(
    private val rootDirectory: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES
) {

    class Entry(
        val directory: File,
        val pages: List<HtmlPage>
    )

    /** Temporary folders of this process start with it; others are leftovers and get deleted. */
    private val temporaryPrefix = "$TEMPORARY_MARKER${UUID.randomUUID()}-"
    private val temporaryCounter = AtomicInteger()

    fun find(file: File, variant: String): Entry? {
        val directory = entryDirectory(file, variant)
        val manifest = File(directory, MANIFEST_NAME)
        if (!manifest.isFile) {
            Log.d(TAG, "find: miss ${file.name} [$variant]")
            return null
        }

        val pages = manifest.readLines()
            .filter { line -> line.isNotBlank() }
            .map { line ->
                HtmlPage(title = line.substringBefore('\t'), file = File(directory, line.substringAfter('\t')))
            }
        if (pages.isEmpty() || !pages.all { page -> page.file.isFile }) {
            Log.w(TAG, "find: broken entry for ${file.name} [$variant], pages=${pages.size}")
            return null
        }

        directory.setLastModified(System.currentTimeMillis()) // Recently used: trimmed last.
        Log.d(TAG, "find: hit ${file.name} [$variant], pages=${pages.size}, dir=${directory.name}")
        return Entry(directory, pages)
    }

    fun store(file: File, variant: String, render: (outputDirectory: File) -> List<HtmlPage>): Entry {
        val target = entryDirectory(file, variant)
        val temporary = newTemporaryDirectory()
        try {
            val pages = render(temporary)
            if (pages.isEmpty()) throw IOException("Rendered document has no pages")
            val manifest = pages.joinToString(separator = "\n") { page ->
                val relativePath = page.file.relativeTo(temporary).invariantSeparatorsPath
                "${page.title.replace('\t', ' ').replace('\n', ' ')}\t$relativePath"
            }
            File(temporary, MANIFEST_NAME).writeText(manifest)

            target.deleteRecursively()
            if (!temporary.renameTo(target)) throw IOException("Can't publish ${target.path}")
        } catch (error: Throwable) {
            Log.w(TAG, "store: render of ${file.name} [$variant] failed, temporary folder removed", error)
            temporary.deleteRecursively()
            throw error
        }
        Log.d(TAG, "store: ${file.name} [$variant] -> ${target.name}")
        trim(keep = target)
        return find(file, variant) ?: throw IOException("Can't read the stored ${target.path}")
    }

    /** Scratch folder for intermediate files; the caller deletes it. */
    fun newWorkDirectory(): File = newTemporaryDirectory()

    private fun newTemporaryDirectory(): File {
        val directory = File(rootDirectory, "$temporaryPrefix${temporaryCounter.incrementAndGet()}")
        if (!directory.mkdirs()) throw IOException("Can't create ${directory.path}")
        return directory
    }

    private fun entryDirectory(file: File, variant: String): File {
        val key = "$CACHE_VERSION|$variant|${file.canonicalPath}|${file.length()}|${file.lastModified()}"
        return File(rootDirectory, sha1(key))
    }

    private fun trim(keep: File) {
        val (temporary, entries) = rootDirectory.listFiles().orEmpty()
            .partition { child -> child.name.startsWith(TEMPORARY_MARKER) }
        val leftovers = temporary.filterNot { child -> child.name.startsWith(temporaryPrefix) }
        leftovers.forEach { leftover -> leftover.deleteRecursively() }

        var totalBytes = 0L
        var removed = 0
        entries.sortedByDescending { entry -> entry.lastModified() }.forEach { entry ->
            totalBytes += entry.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            if (totalBytes > maxBytes && entry != keep) {
                entry.deleteRecursively()
                removed++
            }
        }
        Log.d(TAG, "trim: total=${totalBytes / 1024} KB, entries=${entries.size}, removed=$removed, leftovers=${leftovers.size}")
    }

    private companion object {
        /** Bump when the output layout or renderer configuration changes. */
        const val CACHE_VERSION = 10
        const val MANIFEST_NAME = "pages.tsv"
        const val TEMPORARY_MARKER = "tmp-"
        const val DEFAULT_MAX_BYTES = 200L * 1024 * 1024

        fun sha1(text: String): String {
            return MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
        }
    }
}
