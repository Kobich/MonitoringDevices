package com.android.core.document.impl.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NiceTicksTest {
    @Test
    fun `ticks are round numbers`() {
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), niceTicks(0.0, 4.5))
        assertEquals(listOf(0.0, 50.0, 100.0, 150.0, 200.0, 250.0), niceTicks(0.0, 230.0))
    }

    @Test
    fun `ticks cover the whole range`() {
        listOf(-3.2 to 7.9, 0.01 to 0.07, 120.0 to 121.0, 5.0 to 5.0).forEach { (low, high) ->
            val ticks = niceTicks(low, high)
            assertTrue("$low..$high: $ticks", ticks.size >= 2 && ticks.first() <= low && ticks.last() >= high)
        }
    }
}
