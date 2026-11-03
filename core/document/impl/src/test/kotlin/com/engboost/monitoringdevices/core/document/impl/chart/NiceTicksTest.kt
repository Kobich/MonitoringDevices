package com.engboost.monitoringdevices.core.document.impl.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class NiceTicksTest {
    @Test
    fun `round steps cover the range`() {
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), niceTicks(0.0, 4.3))
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), niceTicks(0.0, 100.0))
        assertEquals(listOf(-10.0, -5.0, 0.0, 5.0, 10.0, 15.0), niceTicks(-7.0, 12.0))
    }

    @Test
    fun `empty range still gives an axis`() {
        val ticks = niceTicks(0.0, 0.0)

        assertEquals(2, ticks.size)
        assertEquals(0.0, ticks.first(), 0.0)
        assertEquals(0.2, ticks.last(), 1e-9)
    }
}
