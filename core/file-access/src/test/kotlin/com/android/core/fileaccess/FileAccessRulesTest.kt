package com.android.core.fileaccess

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class FileAccessRulesTest {
    private val config = FileAccessConfig(actions = emptyMap(), files = emptyMap())

    @Test
    fun `without a source there is no config`() = runTest {
        assertEquals(listOf(FileAccessConfig.Unrestricted), FileAccessRules(emptySet()).config.toList())
    }

    @Test
    fun `null is no config`() = runTest {
        val rules = FileAccessRules(setOf(source(flowOf(null, config))))
        assertEquals(listOf(FileAccessConfig.Unrestricted, config), rules.config.toList())
    }

    @Test
    fun `after an error in the source later configs still come`() = runTest {
        var subscriptions = 0
        val rules = FileAccessRules(setOf(source(flow {
            subscriptions++
            if (subscriptions == 1) throw IOException("The store is broken")
            emit(config)
        })))

        assertEquals(listOf(FileAccessConfig.Unrestricted, config), rules.config.take(2).toList())
        assertEquals(2, subscriptions)
    }

    private fun source(flow: Flow<FileAccessConfig?>) = object : FileAccessConfigSource {
        override val config: Flow<FileAccessConfig?> = flow
    }
}
