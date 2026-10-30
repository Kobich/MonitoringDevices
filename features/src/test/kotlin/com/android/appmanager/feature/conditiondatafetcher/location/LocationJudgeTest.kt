package com.android.appmanager.feature.conditiondatafetcher.location

import com.android.appmanager.feature.conditiondatafetcher.ConditionResult
import com.android.appmanager.feature.conditiondatafetcher.UnknownReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationJudgeTest {
    private val zone = GeoZone(latitude = 55.7558, longitude = 37.6173, radiusMeters = 50.0)
    private val now = 1_000_000L

    @Test
    fun `network fix inside the zone - true at once`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 20.0), now)
        assertEquals(ConditionResult.True, judge.certainResult(now))
    }

    @Test
    fun `far from the zone - false`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 2_000.0, accuracy = 300.0), now)
        assertEquals(ConditionResult.False, judge.certainResult(now))
    }

    @Test
    fun `gps alone is enough`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.GPS, metersNorth = 5.0, accuracy = 5.0), now)
        assertEquals(ConditionResult.True, judge.certainResult(now))
    }

    @Test
    fun `on the zone border - not accurate enough`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.GPS, metersNorth = 50.0, accuracy = 10.0), now)
        assertNull(judge.certainResult(now))
        assertEquals(ConditionResult.Unknown(UnknownReason.NOT_ACCURATE_ENOUGH), judge.finalResult(now))
    }

    @Test
    fun `nothing received - no data`() {
        assertEquals(ConditionResult.Unknown(UnknownReason.NO_DATA), LocationJudge(listOf(zone)).finalResult(now))
    }

    @Test
    fun `fix older than two minutes is dropped`() {
        val judge = LocationJudge(listOf(zone))
        val old = now - LocationJudge.MAX_SAMPLE_AGE_MILLIS - 1

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 20.0, time = old), now)
        assertEquals(ConditionResult.Unknown(UnknownReason.NO_DATA), judge.finalResult(now))
    }

    @Test
    fun `older fix is less certain`() {
        // 10 m accuracy is 20 m effective. 20 s old adds 30 m of walking: 50 m still fits the zone.
        val recent = LocationJudge(listOf(zone))
        recent.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 10.0, time = now - 20_000), now)
        assertEquals(ConditionResult.True, recent.certainResult(now))

        // 60 s old adds 90 m: 110 m no longer fits the 50 m zone.
        val old = LocationJudge(listOf(zone))
        old.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 10.0, time = now - 60_000), now)
        assertNull(old.certainResult(now))
    }

    @Test
    fun `the most accurate fix wins`() {
        val judge = LocationJudge(listOf(zone))

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 200.0), now)
        judge.addSample(sample(LocationSource.GPS, metersNorth = 3.0, accuracy = 5.0), now)
        assertEquals(LocationSource.GPS, judge.bestSample(now)?.source)
        assertEquals(ConditionResult.True, judge.certainResult(now))
    }

    @Test
    fun `fresh coarse fix beats an accurate but old one`() {
        val judge = LocationJudge(listOf(zone))

        // 5 m, 100 s old: 10 + 150 = 160 m effective. 30 m, fresh: 60 m effective.
        judge.addSample(sample(LocationSource.GPS, metersNorth = 0.0, accuracy = 5.0, time = now - 100_000), now)
        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 30.0), now)
        assertEquals(LocationSource.NETWORK, judge.bestSample(now)?.source)
    }

    @Test
    fun `inside one of several zones - true`() {
        val farZone = GeoZone(latitude = 59.9343, longitude = 30.3351, radiusMeters = 50.0)
        val judge = LocationJudge(listOf(farZone, zone))

        judge.addSample(sample(LocationSource.FUSED, metersNorth = 0.0, accuracy = 10.0), now)
        assertEquals(ConditionResult.True, judge.certainResult(now))
    }

    @Test
    fun `every step is traced`() {
        val lines = mutableListOf<String>()
        val judge = LocationJudge(listOf(zone), trace = { lines += it })

        judge.addSample(sample(LocationSource.NETWORK, metersNorth = 0.0, accuracy = 20.0, time = now - 10_000), now)
        judge.addSample(sample(LocationSource.GPS, metersNorth = 0.0, accuracy = 5.0, time = now - 200_000), now)

        assertEquals(
            listOf(
                "NETWORK fix: ±20 m, 10 s old, effective ±55 m, nearest zone center 0 m away, unclear",
                "GPS fix dropped: 200 s old"
            ),
            lines
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
        time: Long = now
    ) = LocationSample(
        source = source,
        latitude = zone.latitude + metersNorth / METERS_PER_DEGREE_LATITUDE,
        longitude = zone.longitude,
        accuracyMeters = accuracy,
        elapsedRealtimeMillis = time
    )

    private companion object {
        const val METERS_PER_DEGREE_LATITUDE = 111_195.0
    }
}
