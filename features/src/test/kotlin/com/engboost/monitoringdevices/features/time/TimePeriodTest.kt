package com.engboost.monitoringdevices.features.time

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TimePeriodTest {
    private val moscow = ZoneId.of("Europe/Moscow")
    private val period = TimePeriod(
        start = ZonedDateTime.of(2026, 10, 24, 9, 0, 0, 0, moscow),
        end = ZonedDateTime.of(2026, 10, 24, 18, 0, 0, 0, moscow)
    )

    @Test
    fun `start is inside, end is outside`() {
        assertTrue(period.start.toInstant() in period)
        assertFalse(period.end.toInstant() in period)
    }

    @Test
    fun `the same moment in another time zone is compared correctly`() {
        // 12:00 in Moscow (UTC+3) is 09:00 UTC.
        val noonInMoscowAsUtc = ZonedDateTime.of(2026, 10, 24, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val eveningUtc = ZonedDateTime.of(2026, 10, 24, 16, 0, 0, 0, ZoneId.of("UTC")).toInstant()

        assertTrue(noonInMoscowAsUtc in period)
        assertFalse(eveningUtc in period)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `end before start is rejected`() {
        TimePeriod(start = period.end, end = period.start)
    }
}
