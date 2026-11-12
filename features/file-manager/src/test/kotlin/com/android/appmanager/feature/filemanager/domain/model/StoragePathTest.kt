package com.android.appmanager.feature.filemanager.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoragePathTest {
    @Test
    fun `root has no parent and no name`() {
        assertTrue(StoragePath.ROOT.isRoot)
        assertNull(StoragePath.ROOT.parent())
        assertEquals("", StoragePath.ROOT.name)
    }

    @Test
    fun `parent and child are inverse`() {
        val path = StoragePath("Samples/Docs")

        assertEquals(StoragePath("Samples"), path.parent())
        assertEquals(StoragePath.ROOT, StoragePath("Samples").parent())
        assertEquals(path, StoragePath("Samples").child("Docs"))
        assertEquals(StoragePath("Samples"), StoragePath.ROOT.child("Samples"))
        assertEquals("Docs", path.name)
    }

    @Test
    fun `hierarchy goes from the root to the path`() {
        assertEquals(
            listOf(StoragePath.ROOT, StoragePath("a"), StoragePath("a/b")),
            StoragePath("a/b").hierarchy()
        )
    }
}
