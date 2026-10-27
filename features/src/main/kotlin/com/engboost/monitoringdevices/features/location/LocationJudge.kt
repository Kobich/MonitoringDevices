package com.engboost.monitoringdevices.features.location

import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.UnknownReason
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Location rules, kept free of Android so they can be unit tested.
 *
 * Effective accuracy = accuracy × [ACCURACY_FACTOR] + walking distance since the fix. The fix with the
 * smallest one wins. Inside when `distance + accuracy <= radius`, outside when `distance - accuracy > radius`.
 */
internal class LocationJudge(
    private val zones: List<GeoZone>,
    private val trace: (String) -> Unit = {}
) {
    private val latest = mutableMapOf<LocationSource, LocationSample>()

    fun addSample(sample: LocationSample, nowMillis: Long) {
        val ageMillis = sample.ageMillis(nowMillis)
        if (ageMillis > MAX_SAMPLE_AGE_MILLIS) {
            trace("${sample.source} fix dropped: ${ageMillis / 1_000} s old")
            return
        }
        latest[sample.source] = sample

        val zoneText = if (zones.isEmpty()) {
            ""
        } else {
            ", nearest zone center ${nearestZoneDistance(sample).roundToInt()} m away, " +
                statusText(certainResult(sample, nowMillis))
        }
        trace(
            "${sample.source} fix: ±${sample.accuracyMeters.roundToInt()} m, ${ageMillis / 1_000} s old, " +
                "effective ±${effectiveAccuracy(sample, nowMillis).roundToInt()} m$zoneText"
        )
    }

    fun bestSample(nowMillis: Long): LocationSample? {
        return latest.values
            .filter { it.ageMillis(nowMillis) <= MAX_SAMPLE_AGE_MILLIS }
            .minByOrNull { effectiveAccuracy(it, nowMillis) }
    }

    /** Null while the best fix is not certain. */
    fun certainResult(nowMillis: Long): ConditionResult? {
        val best = bestSample(nowMillis) ?: return null
        return certainResult(best, nowMillis)
    }

    fun finalResult(nowMillis: Long): ConditionResult {
        val best = bestSample(nowMillis) ?: return ConditionResult.Unknown(UnknownReason.NO_DATA)
        return certainResult(best, nowMillis) ?: ConditionResult.Unknown(UnknownReason.NOT_ACCURATE_ENOUGH)
    }

    private fun LocationSample.ageMillis(nowMillis: Long): Long = (nowMillis - elapsedRealtimeMillis).coerceAtLeast(0)

    private fun effectiveAccuracy(sample: LocationSample, nowMillis: Long): Double {
        val ageSeconds = sample.ageMillis(nowMillis) / 1_000.0
        return sample.accuracyMeters * ACCURACY_FACTOR + ageSeconds * WALKING_SPEED_METERS_PER_SECOND
    }

    private fun certainResult(sample: LocationSample, nowMillis: Long): ConditionResult? {
        val accuracy = effectiveAccuracy(sample, nowMillis)
        val distances = zones.map { zone ->
            zone to distanceMeters(sample.latitude, sample.longitude, zone.latitude, zone.longitude)
        }
        if (distances.any { (zone, distance) -> distance + accuracy <= zone.radiusMeters }) {
            return ConditionResult.True
        }
        val isOutsideAll = distances.all { (zone, distance) -> distance - accuracy > zone.radiusMeters }
        return if (isOutsideAll) ConditionResult.False else null
    }

    private fun nearestZoneDistance(sample: LocationSample): Double {
        return zones.minOf { zone -> distanceMeters(sample.latitude, sample.longitude, zone.latitude, zone.longitude) }
    }

    private fun statusText(result: ConditionResult?): String = when (result) {
        ConditionResult.True -> "certainly inside a zone"
        ConditionResult.False -> "certainly outside all zones"
        else -> "unclear"
    }

    companion object {
        const val MAX_SAMPLE_AGE_MILLIS = 120_000L

        /** Android accuracy has about 68% confidence; doubled it is about 95%. */
        const val ACCURACY_FACTOR = 2.0

        const val WALKING_SPEED_METERS_PER_SECOND = 1.5

        private const val EARTH_RADIUS_METERS = 6_371_000.0

        /** Great-circle distance (haversine). */
        fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
        }
    }
}
