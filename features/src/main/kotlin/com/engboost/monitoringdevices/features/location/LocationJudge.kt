package com.engboost.monitoringdevices.features.location

import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.UnknownReason
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decides whether the user is inside any of [zones] from GPS and network location samples.
 * Plain Kotlin without Android, so the rules can be unit tested.
 *
 * Rules:
 * - Mock samples and samples older than [MAX_SAMPLE_AGE_MILLIS] are ignored.
 * - A sample is certainly inside a zone when `distance + accuracy <= radius`,
 *   certainly outside when `distance - accuracy > radius`, otherwise unclear.
 * - GPS and network are checked against each other: if the distance between them is larger than
 *   their accuracies together, at least one of them is wrong ([UnknownReason.SOURCES_CONFLICT]).
 *   GPS can be spoofed, network location is not affected by GPS jamming.
 * - GPS alone is never trusted ([UnknownReason.NOT_VERIFIED]); network alone is.
 */
internal class LocationJudge(private val zones: List<GeoZone>) {
    private val latest = mutableMapOf<LocationSource, LocationSample>()
    private var isMockReceived = false

    /** Adds [sample] and returns the result once it is certain, or null to keep waiting. */
    fun addSample(sample: LocationSample, nowMillis: Long): ConditionResult? {
        if (sample.isMock) {
            isMockReceived = true
            return null
        }
        if (sample.isStale(nowMillis)) return null
        latest[sample.source] = sample

        val gps = freshSample(LocationSource.GPS, nowMillis) ?: return null
        val network = freshSample(LocationSource.NETWORK, nowMillis) ?: return null
        if (isConflict(gps, network)) return ConditionResult.Unknown(UnknownReason.SOURCES_CONFLICT)
        return certainResult(gps) ?: certainResult(network)
    }

    /** The result when the waiting window is over. */
    fun finalResult(nowMillis: Long): ConditionResult {
        val gps = freshSample(LocationSource.GPS, nowMillis)
        val network = freshSample(LocationSource.NETWORK, nowMillis)
        return when {
            gps != null && network != null -> when {
                isConflict(gps, network) -> ConditionResult.Unknown(UnknownReason.SOURCES_CONFLICT)
                else -> certainResult(gps)
                    ?: certainResult(network)
                    ?: ConditionResult.Unknown(UnknownReason.NOT_ACCURATE_ENOUGH)
            }

            network != null -> certainResult(network)
                ?: ConditionResult.Unknown(UnknownReason.NOT_ACCURATE_ENOUGH)

            gps != null -> ConditionResult.Unknown(UnknownReason.NOT_VERIFIED)
            isMockReceived -> ConditionResult.Unknown(UnknownReason.MOCK_DATA)
            else -> ConditionResult.Unknown(UnknownReason.NO_DATA)
        }
    }

    private fun freshSample(source: LocationSource, nowMillis: Long): LocationSample? {
        return latest[source]?.takeUnless { it.isStale(nowMillis) }
    }

    private fun LocationSample.isStale(nowMillis: Long): Boolean {
        return nowMillis - elapsedRealtimeMillis > MAX_SAMPLE_AGE_MILLIS
    }

    private fun isConflict(gps: LocationSample, network: LocationSample): Boolean {
        val distance = distanceMeters(gps.latitude, gps.longitude, network.latitude, network.longitude)
        return distance > gps.accuracyMeters + network.accuracyMeters
    }

    /** True if certainly inside any zone, false if certainly outside all of them, null if unclear. */
    private fun certainResult(sample: LocationSample): ConditionResult? {
        val distances = zones.map { zone ->
            zone to distanceMeters(sample.latitude, sample.longitude, zone.latitude, zone.longitude)
        }
        val isInsideAny = distances.any { (zone, distance) ->
            distance + sample.accuracyMeters <= zone.radiusMeters
        }
        if (isInsideAny) return ConditionResult.True

        val isOutsideAll = distances.all { (zone, distance) ->
            distance - sample.accuracyMeters > zone.radiusMeters
        }
        return if (isOutsideAll) ConditionResult.False else null
    }

    companion object {
        /** A sample older than this no longer describes where the user is now. */
        const val MAX_SAMPLE_AGE_MILLIS = 10_000L

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
