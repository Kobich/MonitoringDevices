package com.engboost.monitoringdevices.core.document.impl.cache

import com.engboost.monitoringdevices.core.document.api.document.HtmlPage
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Converted documents on disk: one folder per (file version, variant) with a small manifest
 * of pages. A folder appears atomically (rename), so an interrupted conversion leaves nothing.
 */
internal class RenderCache(private val rootDirectory: File) {

    class Entry(
        val directory: File,
        val pages: List<HtmlPage>
    )

    fun find(file: File, variant: String): Entry? {
        val directory = entryDirectory(file, variant)
        val manifest = File(directory, MANIFEST_NAME)
        if (!manifest.isFile) return null

        val pages = manifest.readLines()
            .filter { line -> line.isNotBlank() }
            .map { line ->
                HtmlPage(title = line.substringBefore('\t'), file = File(directory, line.substringAfter('\t')))
            }
        return Entry(directory, pages).takeIf { pages.isNotEmpty() && pages.all { page -> page.file.isFile } }
    }

    /** Runs [render] in an empty temporary folder; on success the folder becomes the entry. */
    fun store(file: File, variant: String, render: (outputDirectory: File) -> List<HtmlPage>): Entry {
        val target = entryDirectory(file, variant)
        val temporary = File(rootDirectory, "${target.name}.tmp-${System.nanoTime()}")
        if (!temporary.mkdirs()) throw IOException("Can't create ${temporary.path}")

        try {
            val manifest = render(temporary).joinToString(separator = "\n") { page ->
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
        return find(file, variant) ?: throw IOException("Rendered document has no pages")
    }

    /** Scratch folder for intermediate files; the caller deletes it. */
    fun newWorkDirectory(): File = File(rootDirectory, "work-${System.nanoTime()}").apply { mkdirs() }

    /** A changed file (size or modification time) gets a new entry. */
    private fun entryDirectory(file: File, variant: String): File {
        val key = "$CACHE_VERSION|$variant|${file.canonicalPath}|${file.length()}|${file.lastModified()}"
        val hash = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return File(rootDirectory, hash)
    }

    private companion object {
        /** Bump when the output layout or renderer configuration changes. */
        const val CACHE_VERSION = 3
        const val MANIFEST_NAME = "pages.tsv"
    }
}
