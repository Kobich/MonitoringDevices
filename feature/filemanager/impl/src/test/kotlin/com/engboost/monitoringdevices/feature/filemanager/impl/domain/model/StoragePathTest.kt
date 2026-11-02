package com.engboost.monitoringdevices.feature.filemanager.impl.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoragePathTest {
    @Test
    fun `root has no parent and no segments`() {
        assertTrue(StoragePath.ROOT.isRoot)
        assertNull(StoragePath.ROOT.parent())
        assertEquals(emptyList<String>(), StoragePath.ROOT.segments)
        assertEquals("/", StoragePath.ROOT.toString())
    }

    @Test
    fun `child and parent are symmetric`() {
        val path = StoragePath.ROOT.child("docs").child("report.pdf")

        assertEquals("docs/report.pdf", path.value)
        assertEquals("report.pdf", path.name)
        assertEquals(StoragePath("docs"), path.parent())
        assertEquals(StoragePath.ROOT, path.parent()?.parent())
    }

    @Test
    fun `hierarchy goes from root to the path`() {
        val hierarchy = StoragePath("a/b/c").hierarchy()

        assertEquals(
            listOf(StoragePath.ROOT, StoragePath("a"), StoragePath("a/b"), StoragePath("a/b/c")),
            hierarchy
        )
    }
}
