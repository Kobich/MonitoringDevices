package com.engboost.monitoringdevices.feature.filemanager.impl.data.source

import com.engboost.monitoringdevices.feature.filemanager.impl.domain.model.StoragePath
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class InternalFileSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var root: File
    private lateinit var source: InternalFileSource

    @Before
    fun setUp() {
        root = File(temporaryFolder.root, "files")
        source = InternalFileSource(root)
    }

    @Test
    fun `resolves paths inside the root`() {
        assertEquals(root.canonicalFile, source.resolve(StoragePath.ROOT))
        assertEquals(File(root, "a/b.txt").canonicalFile, source.resolve(StoragePath("a/b.txt")))
    }

    @Test(expected = IOException::class)
    fun `rejects paths escaping the root`() {
        source.resolve(StoragePath("../outside"))
    }

    @Test(expected = IOException::class)
    fun `rejects sibling folder with the same prefix`() {
        source.resolve(StoragePath("../files-other/secret"))
    }

    @Test
    fun `unique child appends index before extension`() {
        val directory = source.resolve(StoragePath.ROOT)
        File(directory, "report.pdf").createNewFile()
        File(directory, "report (1).pdf").createNewFile()
        File(directory, "photos").mkdir()

        assertEquals("report (2).pdf", source.uniqueChild(directory, "report.pdf").name)
        assertEquals("photos (1)", source.uniqueChild(directory, "photos").name)
        assertEquals("new.txt", source.uniqueChild(directory, "new.txt").name)
        assertEquals(".nomedia (1)", source.uniqueChild(directory.also { File(it, ".nomedia").createNewFile() }, ".nomedia").name)
    }

    @Test
    fun `sanitizes external names`() {
        assertEquals("a_b_c", InternalFileSource.sanitizeFileName("a/b\\c"))
        assertEquals("unnamed", InternalFileSource.sanitizeFileName(".."))
        assertEquals("unnamed", InternalFileSource.sanitizeFileName("   "))
        assertEquals("name", InternalFileSource.sanitizeFileName("  name "))
    }
}
