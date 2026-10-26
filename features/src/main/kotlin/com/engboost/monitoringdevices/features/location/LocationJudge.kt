package com.engboost.monitoringdevices.features.location

import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.UnknownReason
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decides whether the user is inside any of [zones] from GPS and network location samples.
 * Plain Kotlin without Android, so the rules can be unit tested.
 *
 * Rules:
 * - Mock samples and samples older than [MAX_SAMPLE_AGE_MILLIS] are ignored.
 * - Android accuracy is a radius with about 68% confidence, so it is multiplied by
 *   [ACCURACY_FACTOR] (about 95%). Below, "accuracy" means this multiplied value.
 * - A sample is certainly inside a zone when `distance + accuracy <= radius`,
 *   certainly outside when `distance - accuracy > radius`, otherwise unclear.
 * - GPS and network are checked against each other. GPS can be spoofed, network location is not
 *   affected by GPS jamming. They disagree when one is certainly inside and the other certainly
 *   outside, or when the distance between them is larger than their accuracies together plus how
 *   far the user could move between the two fixes at [MAX_USER_SPEED_METERS_PER_SECOND].
 * - A single disagreement may be a bad fix, so it only pauses the decision. A disagreement counts
 *   again only when both samples are new, so one bad fix is not counted several times.
 *   [CONFLICT_CONFIRMATIONS] disagreements in a row, or a disagreement when the window is over,
 *   give [UnknownReason.SOURCES_CONFLICT].
 * - GPS alone is never trusted ([UnknownReason.NOT_VERIFIED] when network location did not arrive);
 *   network alone is.
 *   When GPS cannot arrive at all ([isGpsExpected] is false), network decides without waiting.
 *
 * Every step is reported to [trace] as a readable line.
 */
internal class LocationJudge(
    private val zones: List<GeoZone>,
    private val isGpsExpected: Boolean = true,
    private val trace: (String) -> Unit = {}
) {
    private val latest = mutableMapOf<LocationSource, LocationSample>()
    private var isMockReceived = false
    private var conflictsInRow = 0
    private var lastCountedConflict: Pair<LocationSample, LocationSample>? = null

    /** Adds [sample] and returns the result once it is certain, or null to keep waiting. */
    fun addSample(sample: LocationSample, nowMillis: Long): ConditionResult? {
        val ageMillis = nowMillis - sample.elapsedRealtimeMillis
        if (sample.isMock) {
            trace("${sample.source} fix dropped: mock location")
            isMockReceived = true
            return null
        }
        if (sample.isStale(nowMillis)) {
            trace("${sample.source} fix dropped: $ageMillis ms old")
            return null
        }
        latest[sample.source] = sample
        trace(
            "${sample.source} fix: ±${sample.accuracyMeters.roundToInt()} m, $ageMillis ms old, " +
                "nearest zone center ${nearestZoneDistance(sample).roundToInt()} m away, ${sample.statusText()}"
        )

        val network = freshSample(LocationSource.NETWORK, nowMillis) ?: run {
            trace("Waiting for network location")
            return null
        }
        val gps = freshSample(LocationSource.GPS, nowMillis) ?: run {
            if (isGpsExpected) {
                trace("Waiting for GPS")
                return null
            }
            return certainResult(network)
        }

        if (isDisagreement(gps, network)) {
            countDisagreement(gps, network)
            trace("GPS and network disagree ($conflictsInRow of $CONFLICT_CONFIRMATIONS)")
            return if (conflictsInRow >= CONFLICT_CONFIRMATIONS) {
                ConditionResult.Unknown(UnknownReason.SOURCES_CONFLICT)
            } else {
                null
            }
        }
        conflictsInRow = 0
        lastCountedConflict = null
        return certainResult(gps) ?: certainResult(network)
    }

    /** The result when the waiting window is over. */
    fun finalResult(nowMillis: Long): ConditionResult {
        val gps = freshSample(LocationSource.GPS, nowMillis)
        val network = freshSample(LocationSource.NETWORK, nowMillis)
        return when {
            gps != null && network != null -> when {
                isDisagreement(gps, network) -> ConditionResult.Unknown(UnknownReason.SOURCES_CONFLICT)
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

    private fun nearestZoneDistance(sample: LocationSample): Double {
        return zones.minOf { zone -> distanceMeters(sample.latitude, sample.longitude, zone.latitude, zone.longitude) }
    }

    private fun LocationSample.statusText(): String = when (certainResult(this)) {
        ConditionResult.True -> "certainly inside a zone"
        ConditionResult.False -> "certainly outside all zones"
        else -> "unclear"
    }

    private fun freshSample(source: LocationSource, nowMillis: Long): LocationSample? {
        return latest[source]?.takeUnless { it.isStale(nowMillis) }
    }

    private fun LocationSample.isStale(nowMillis: Long): Boolean {
        return nowMillis - elapsedRealtimeMillis > MAX_SAMPLE_AGE_MILLIS
    }

    private val LocationSample.accuracy: Double
        get() = accuracyMeters * ACCURACY_FACTOR

    private fun isDisagreement(gps: LocationSample, network: LocationSample): Boolean {
        val gpsResult = certainResult(gps)
        val networkResult = certainResult(network)
        if (gpsResult != null && networkResult != null && gpsResult != networkResult) return true
        return isTooFarApart(gps, network)
    }

    private fun countDisagreement(gps: LocationSample, network: LocationSample) {
        val last = lastCountedConflict
        if (last == null || (last.first != gps && last.second != network)) {
            conflictsInRow++
            lastCountedConflict = gps to network
        }
    }

    private fun isTooFarApart(gps: LocationSample, network: LocationSample): Boolean {
        val distance = distanceMeters(gps.latitude, gps.longitude, network.latitude, network.longitude)
        val timeGapSeconds = abs(gps.elapsedRealtimeMillis - network.elapsedRealtimeMillis) / 1_000.0
        val possibleMovement = timeGapSeconds * MAX_USER_SPEED_METERS_PER_SECOND
        return distance > gps.accuracy + network.accuracy + possibleMovement
    }

    /** True if certainly inside any zone, false if certainly outside all of them, null if unclear. */
    private fun certainResult(sample: LocationSample): ConditionResult? {
        val distances = zones.map { zone ->
            zone to distanceMeters(sample.latitude, sample.longitude, zone.latitude, zone.longitude)
        }
        val isInsideAny = distances.any { (zone, distance) ->
            distance + sample.accuracy <= zone.radiusMeters
        }
        if (isInsideAny) return ConditionResult.True

        val isOutsideAll = distances.all { (zone, distance) ->
            distance - sample.accuracy > zone.radiusMeters
        }
        return if (isOutsideAll) ConditionResult.False else null
    }

    companion object {
        /** A sample older than this no longer describes where the user is now. */
        const val MAX_SAMPLE_AGE_MILLIS = 10_000L

        /** Turns Android accuracy (about 68% confidence) into about 95% confidence. */
        const val ACCURACY_FACTOR = 2.0

        /** How many GPS and network disagreements in a row count as a conflict. */
        const val CONFLICT_CONFIRMATIONS = 3

        /** Fastest the user is expected to move, about 108 km/h. */
        const val MAX_USER_SPEED_METERS_PER_SECOND = 30.0

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
