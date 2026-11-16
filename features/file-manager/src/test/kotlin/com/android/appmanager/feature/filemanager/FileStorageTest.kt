package com.android.appmanager.feature.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FileStorageTest {

    @Test
    fun `valid names are kept as they are`() {
        listOf("reports", "device-logs", "photos_2026", "a", "0", "x".repeat(64)).forEach { name ->
            assertEquals(name, FileStorage.named(name).name)
        }
    }

    @Test
    fun `names that are not a single safe folder are rejected`() {
        listOf("", ".", "..", "-logs", "_logs", "Reports", "my reports", "a/b", "a\\b", "отчёты", "x".repeat(65))
            .forEach { name ->
                assertThrows(name, IllegalArgumentException::class.java) { FileStorage.named(name) }
            }
    }

    @Test
    fun `storages with the same name are equal`() {
        assertEquals(FileStorage.named("reports"), FileStorage.named("reports"))
        assertEquals(FileStorage.Default, FileStorage.named(FileStorage.Default.name))
    }
}
