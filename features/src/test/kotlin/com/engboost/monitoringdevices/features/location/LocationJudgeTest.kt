package com.engboost.monitoringdevices.features.location

import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.UnknownReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationJudgeTest {
    private val zone = GeoZone(latitude = 55.7558, longitude = 37.6173, radiusMeters = 50.0)
    private val now = 100_000L

    @Test
    fun `gps and network agree inside - true`() {
        val judge = LocationJudge(listOf(zone))

        assertNull(judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 40.0), now))
        assertEquals(
            ConditionResult.True,
            judge.addSample(sample(LocationSource.GPS, metersNorth = 5.0, accuracy = 10.0), now)
        )
    }

    @Test
    fun `far from zone - false`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 2_000.0, accuracy = 300.0), now)
        assertEquals(
            ConditionResult.False,
            judge.addSample(sample(LocationSource.GPS, metersNorth = 2_010.0, accuracy = 20.0), now)
        )
    }

    @Test
    fun `gps far from network several times in a row - sources conflict`() {
        val judge = LocationJudge(listOf(zone))
        val spoofedGps = sample(LocationSource.GPS, metersNorth = 10_000.0, accuracy = 5.0)

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 40.0), now)
        assertNull(judge.addSample(spoofedGps, now))
        assertNull(judge.addSample(spoofedGps, now))
        assertEquals(
            ConditionResult.Unknown(UnknownReason.SOURCES_CONFLICT),
            judge.addSample(spoofedGps, now)
        )
    }

    @Test
    fun `single bad gps fix does not stop the check`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 20.0), now)
        assertNull(judge.addSample(sample(LocationSource.GPS, metersNorth = 1_000.0, accuracy = 5.0), now))
        assertEquals(
            ConditionResult.True,
            judge.addSample(sample(LocationSource.GPS, metersNorth = 0.0, accuracy = 5.0), now)
        )
    }

    @Test
    fun `disagreement when the window is over - sources conflict`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 20.0), now)
        judge.addSample(sample(LocationSource.GPS, metersNorth = 1_000.0, accuracy = 5.0), now)
        assertEquals(ConditionResult.Unknown(UnknownReason.SOURCES_CONFLICT), judge.finalResult(now))
    }

    @Test
    fun `fixes taken apart in time allow for movement`() {
        val judge = LocationJudge(listOf(zone))

        // 8 seconds apart at up to 30 m/s allows 240 m of movement on top of the accuracies.
        judge.addSample(
            sample(LocationSource.NETWORK, metersNorth = 3_000.0, accuracy = 20.0, time = now - 8_000),
            now
        )
        assertEquals(
            ConditionResult.False,
            judge.addSample(sample(LocationSource.GPS, metersNorth = 3_250.0, accuracy = 5.0), now)
        )
    }

    @Test
    fun `accuracy is doubled for certainty`() {
        val judge = LocationJudge(listOf(zone))

        // 30 + 15 = 45 m would fit the 50 m zone, but 30 + 15 * 2 = 60 m does not.
        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 30.0, accuracy = 15.0), now)
        assertEquals(
            ConditionResult.Unknown(UnknownReason.NOT_ACCURATE_ENOUGH),
            judge.finalResult(now)
        )
    }

    @Test
    fun `on the zone border - waits, then not accurate enough`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 50.0, accuracy = 40.0), now)
        assertNull(judge.addSample(sample(LocationSource.GPS, metersNorth = 50.0, accuracy = 10.0), now))
        assertEquals(
            ConditionResult.Unknown(UnknownReason.NOT_ACCURATE_ENOUGH),
            judge.finalResult(now)
        )
    }

    @Test
    fun `gps alone - not verified`() {
        val judge = LocationJudge(listOf(zone))

        assertNull(judge.addSample(sample(LocationSource.GPS, metersNorth = 0.0, accuracy = 5.0), now))
        assertEquals(ConditionResult.Unknown(UnknownReason.NOT_VERIFIED), judge.finalResult(now))
    }

    @Test
    fun `network alone and certain - trusted`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 20.0), now)
        assertEquals(ConditionResult.True, judge.finalResult(now))
    }

    @Test
    fun `nothing received - no data`() {
        assertEquals(
            ConditionResult.Unknown(UnknownReason.NO_DATA),
            LocationJudge(listOf(zone)).finalResult(now)
        )
    }

    @Test
    fun `only mock samples - mock data`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.GPS, metersNorth = 0.0, accuracy = 5.0, isMock = true), now)
        assertEquals(ConditionResult.Unknown(UnknownReason.MOCK_DATA), judge.finalResult(now))
    }

    @Test
    fun `stale samples are ignored`() {
        val judge = LocationJudge(listOf(zone))
        val old = now - LocationJudge.MAX_SAMPLE_AGE_MILLIS - 1

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 20.0, time = old), now)
        assertEquals(ConditionResult.Unknown(UnknownReason.NO_DATA), judge.finalResult(now))
    }

    @Test
    fun `inside one of several zones - true`() {
        val farZone = GeoZone(latitude = 59.9343, longitude = 30.3351, radiusMeters = 50.0)
        val judge = LocationJudge(listOf(farZone, zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 30.0), now)
        assertEquals(
            ConditionResult.True,
            judge.addSample(sample(LocationSource.GPS, metersNorth = 0.0, accuracy = 10.0), now)
        )
    }

    @Test
    fun `distance is about right`() {
        // One degree of latitude is about 111.2 km.
        val distance = LocationJudge.distanceMeters(55.0, 37.0, 56.0, 37.0)
        assertEquals(111_195.0, distance, 100.0)
    }

    private fun sample(
        source: LocationSource,
        metersNorth: Double,
        accuracy: Double,
        isMock: Boolean = false,
        time: Long = now
    ) = LocationSample(
        source = source,
        latitude = zone.latitude + metersNorth / METERS_PER_DEGREE_LATITUDE,
        longitude = zone.longitude,
        accuracyMeters = accuracy,
        elapsedRealtimeMillis = time,
        isMock = isMock
    )

    private companion object {
        const val METERS_PER_DEGREE_LATITUDE = 111_195.0
    }
}
