package com.android.appmanager.feature.conditiondatafetcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TimeConditionTest {
    private val period = TimeCondition(start = "2026-10-24T09:00:00+03:00", end = "2026-10-24T18:00:00+03:00")

    @Test
    fun `start is inside, end is outside`() {
        assertTrue(Instant.parse("2026-10-24T06:00:00Z") in period)
        assertFalse(Instant.parse("2026-10-24T15:00:00Z") in period)
    }

    @Test
    fun `the same moment in another time zone is compared correctly`() {
        // 12:00 in Moscow (UTC+3) is 09:00 UTC, 19:00 in Moscow is 16:00 UTC.
        assertTrue(Instant.parse("2026-10-24T09:00:00Z") in period)
        assertFalse(Instant.parse("2026-10-24T16:00:00Z") in period)
    }

    @Test
    fun `utc and zone id forms are accepted`() {
        val utc = TimeCondition(start = "2026-10-24T06:00:00Z", end = "2026-10-24T15:00:00Z")
        val zoneId = TimeCondition(
            start = "2026-10-24T09:00:00+03:00[Europe/Moscow]",
            end = "2026-10-24T18:00:00+03:00[Europe/Moscow]"
        )

        assertTrue(Instant.parse("2026-10-24T09:00:00Z") in utc)
        assertTrue(Instant.parse("2026-10-24T09:00:00Z") in zoneId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `time without a time zone is rejected`() {
        TimeCondition(start = "2026-10-24T09:00:00", end = "2026-10-24T18:00:00")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `end before start is rejected`() {
        TimeCondition(start = period.end, end = period.start)
    }
}
