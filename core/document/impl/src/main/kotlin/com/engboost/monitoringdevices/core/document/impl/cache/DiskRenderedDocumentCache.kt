package com.engboost.monitoringdevices.core.document.impl.cache

import com.engboost.monitoringdevices.core.document.api.cache.RenderedDocumentCache
import com.engboost.monitoringdevices.core.document.api.document.HtmlPage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Converted documents on disk: one folder per (source identity, variant) with
 * a small manifest of pages. Folders are published atomically by rename, so a crash
 * mid-conversion never leaves a half-written entry behind.
 */
internal class DiskRenderedDocumentCache(
    private val rootDirectory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RenderedDocumentCache {

    class Entry(
        val directory: File,
        val pages: List<HtmlPage>
    )

    fun find(identity: String, variant: String): Entry? {
        val directory = entryDirectory(identity, variant)
        val manifest = File(directory, MANIFEST_NAME)
        if (!manifest.isFile) return null

        val pages = manifest.readLines()
            .filter { line -> line.isNotBlank() }
            .map { line ->
                val title = line.substringBefore('\t')
                val relativePath = line.substringAfter('\t')
                HtmlPage(title = title, file = File(directory, relativePath))
            }
        return Entry(directory, pages).takeIf { pages.isNotEmpty() && pages.all { page -> page.file.isFile } }
    }

    /**
     * Runs [render] in an empty temporary folder; it returns the pages it produced.
     * On success the folder becomes the cache entry.
     */
    fun store(identity: String, variant: String, render: (outputDirectory: File) -> List<HtmlPage>): Entry {
        val target = entryDirectory(identity, variant)
        val temporary = File(rootDirectory, "${target.name}.tmp-${System.nanoTime()}")
        temporary.deleteRecursively()
        if (!temporary.mkdirs()) throw IOException("Can't create ${temporary.path}")

        try {
            val pages = render(temporary)
            val manifest = pages.joinToString(separator = "\n") { page ->
                val relativePath = page.file.relativeTo(temporary).invariantSeparatorsPath
                "${page.title.replace('\t', ' ').replace('\n', ' ')}\t$relativePath"
            }
            File(temporary, MANIFEST_NAME).writeText(manifest)

            target.deleteRecursively()
            if (!temporary.renameTo(target)) throw IOException("Can't publish ${target.path}")
        } catch (error: Throwable) {
            temporary.deleteRecursively()
            throw error
        }
        return find(identity, variant) ?: throw IOException("Rendered document has no pages")
    }

    /** Scratch folder for intermediate files (copied sources, preprocessed documents). */
    fun newWorkDirectory(): File {
        return File(rootDirectory, "work-${System.nanoTime()}").also { directory -> directory.mkdirs() }
    }

    override suspend fun sizeBytes(): Long = withContext(ioDispatcher) {
        rootDirectory.walkBottomUp().filter { file -> file.isFile }.sumOf { file -> file.length() }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            rootDirectory.listFiles()?.forEach { file -> file.deleteRecursively() }
        }
    }

    private fun entryDirectory(identity: String, variant: String): File {
        return File(rootDirectory, sha1("$CACHE_VERSION|$variant|$identity"))
    }

    private fun sha1(value: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private companion object {
        /** Bump when the output layout or renderer configuration changes. */
        const val CACHE_VERSION = 3
        const val MANIFEST_NAME = "pages.tsv"
    }
}
