package com.engboost.monitoringdevices.core.document.impl.cache

import com.engboost.monitoringdevices.core.document.api.document.HtmlPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class RenderCacheTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val root by lazy { temporaryFolder.newFolder("cache") }

    @Test
    fun `stored entry is found again`() {
        val cache = RenderCache(root)
        val source = source("a.docx")

        val stored = cache.store(source, "as-is", ::render)
        val found = cache.find(source, "as-is")

        assertNotNull(found)
        assertEquals(stored.directory, found!!.directory)
        assertEquals(listOf("Sheet 1"), found.pages.map { it.title })
        assertEquals("<html>page</html>", found.pages.single().file.readText())
    }

    @Test
    fun `another variant or a changed file misses the cache`() {
        val cache = RenderCache(root)
        val source = source("a.docx")
        cache.store(source, "as-is", ::render)

        assertNull(cache.find(source, "as-is|password:secret"))

        source.appendText("changed")
        assertNull(cache.find(source, "as-is"))
    }

    @Test
    fun `failed render leaves nothing behind`() {
        val cache = RenderCache(root)
        try {
            cache.store(source("a.docx"), "as-is") { throw IOException("broken") }
            fail()
        } catch (expected: IOException) {
        }
        try {
            cache.store(source("b.docx"), "as-is") { emptyList() }
            fail()
        } catch (expected: IOException) {
        }

        assertTrue(root.list()!!.isEmpty())
    }

    @Test
    fun `leftovers of a killed process are removed, own work folders are kept`() {
        val leftover = File(root, "tmp-previous-process-1").apply { mkdirs() }
        val cache = RenderCache(root)
        val ownWork = cache.newWorkDirectory()

        cache.store(source("a.docx"), "as-is", ::render)

        assertFalse(leftover.exists())
        assertTrue(ownWork.exists())
    }

    @Test
    fun `least recently used entries are removed above the limit`() {
        val cache = RenderCache(root, maxBytes = 1)
        val old = cache.store(source("old.docx"), "as-is", ::render)
        old.directory.setLastModified(System.currentTimeMillis() - 60_000)

        val new = cache.store(source("new.docx"), "as-is", ::render)

        assertFalse(old.directory.exists())
        assertTrue("the new entry is kept even above the limit", new.directory.exists())
    }

    private fun source(name: String) = temporaryFolder.newFile(name).apply { writeText(name) }

    private fun render(directory: File): List<HtmlPage> {
        val page = File(directory, "sheet1.html").apply { writeText("<html>page</html>") }
        return listOf(HtmlPage(title = "Sheet 1", file = page))
    }
}
