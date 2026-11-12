package com.android.appmanager.feature.filemanager.data.source

import com.android.appmanager.feature.filemanager.domain.model.StoragePath
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.nio.file.Files

class InternalFileSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val root by lazy { File(folder.root, "storage") }
    private val source by lazy { InternalFileSource(root) }

    @Test
    fun `the root is created on first use`() {
        assertEquals(root.canonicalFile, source.resolve(StoragePath.ROOT))
        assertEquals(true, root.isDirectory)
    }

    @Test
    fun `a nested path stays inside the root`() {
        assertEquals(File(root, "Media/a.png").canonicalFile, source.resolve(StoragePath("Media/a.png")))
    }

    @Test(expected = IOException::class)
    fun `dot-dot can't leave the root`() {
        source.resolve(StoragePath("../outside.txt"))
    }

    @Test(expected = IOException::class)
    fun `a symlink can't leave the root`() {
        source.resolve(StoragePath.ROOT)
        val outside = folder.newFolder("outside")
        val link = File(root, "link").toPath()
        val created = runCatching { Files.createSymbolicLink(link, outside.toPath()) }.isSuccess
        assumeTrue("symlinks aren't available here", created)

        source.resolve(StoragePath("link/secret.txt"))
    }

    @Test
    fun `a taken name gets a number before the extension`() {
        val directory = source.resolve(StoragePath.ROOT)
        File(directory, "report.pdf").createNewFile()
        File(directory, "report (1).pdf").createNewFile()
        File(directory, "notes").createNewFile()
        File(directory, ".hidden").createNewFile()

        assertEquals("report (2).pdf", source.uniqueChild(directory, "report.pdf").name)
        assertEquals("notes (1)", source.uniqueChild(directory, "notes").name)
        assertEquals(".hidden (1)", source.uniqueChild(directory, ".hidden").name)
        assertEquals("new.txt", source.uniqueChild(directory, "new.txt").name)
    }

    @Test
    fun `file names are cleaned`() {
        assertEquals("a_b_c", InternalFileSource.sanitizeFileName("a/b\\c"))
        assertEquals("tab_name", InternalFileSource.sanitizeFileName("tab\tname"))
        assertEquals("trimmed", InternalFileSource.sanitizeFileName("  trimmed  "))
        assertEquals("unnamed", InternalFileSource.sanitizeFileName(""))
        assertEquals("unnamed", InternalFileSource.sanitizeFileName(".."))
        assertEquals(200, InternalFileSource.sanitizeFileName("x".repeat(300)).length)
    }
}
