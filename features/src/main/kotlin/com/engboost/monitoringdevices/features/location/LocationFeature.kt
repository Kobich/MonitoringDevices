package com.engboost.monitoringdevices.features.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.SystemClock
import com.engboost.monitoringdevices.features.ConditionFeature
import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.UnknownReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Checks whether the user is inside any of the given zones.
 *
 * Listens to GPS and network location (Wi-Fi and cell towers) at the same time for up to
 * [LOCATION_WINDOW] and stops as soon as the answer is certain. How samples are judged
 * is described in [LocationJudge].
 *
 * Unknown reasons: [UnknownReason.PERMISSION_DENIED], [UnknownReason.SOURCE_UNAVAILABLE],
 * [UnknownReason.SOURCE_DISABLED], [UnknownReason.NO_DATA], [UnknownReason.MOCK_DATA],
 * [UnknownReason.NOT_ACCURATE_ENOUGH], [UnknownReason.SOURCES_CONFLICT], [UnknownReason.NOT_VERIFIED].
 *
 * In the background it needs a foreground service of type `location`.
 */
@SuppressLint("MissingPermission")
class LocationFeature(context: Context) : ConditionFeature<List<GeoZone>> {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    override suspend fun isMeetsCondition(input: List<GeoZone>): ConditionResult {
        if (input.isEmpty()) return ConditionResult.False
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return ConditionResult.Unknown(UnknownReason.PERMISSION_DENIED)
        }
        val manager = locationManager ?: return ConditionResult.Unknown(UnknownReason.SOURCE_UNAVAILABLE)
        if (!manager.isLocationEnabled) return ConditionResult.Unknown(UnknownReason.SOURCE_DISABLED)

        val providers = PROVIDERS.filter { manager.hasProvider(it) && manager.isProviderEnabled(it) }
        if (providers.isEmpty()) return ConditionResult.Unknown(UnknownReason.SOURCE_UNAVAILABLE)
        // GPS alone is never trusted, so without network location there is no point in turning GPS on.
        if (LocationManager.NETWORK_PROVIDER !in providers) {
            return ConditionResult.Unknown(UnknownReason.NOT_VERIFIED)
        }

        val judge = LocationJudge(input, isGpsExpected = LocationManager.GPS_PROVIDER in providers)
        val samples = Channel<LocationSample>(Channel.UNLIMITED)
        val listeners = providers.associateWith { provider ->
            LocationListener { location -> location.toSample(provider)?.let { samples.trySend(it) } }
        }
        val request = LocationRequest.Builder(SAMPLE_INTERVAL_MILLIS)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()

        return try {
            listeners.forEach { (provider, listener) ->
                manager.requestLocationUpdates(provider, request, appContext.mainExecutor, listener)
            }
            val certainResult = withTimeoutOrNull(LOCATION_WINDOW) {
                for (sample in samples) {
                    judge.addSample(sample, SystemClock.elapsedRealtime())?.let { return@withTimeoutOrNull it }
                }
                null
            }
            certainResult ?: judge.finalResult(SystemClock.elapsedRealtime())
        } catch (error: SecurityException) {
            ConditionResult.Unknown(UnknownReason.PERMISSION_DENIED)
        } finally {
            listeners.values.forEach { runCatching { manager.removeUpdates(it) } }
        }
    }

    private fun Location.toSample(provider: String): LocationSample? {
        if (!hasAccuracy()) return null
        return LocationSample(
            source = if (provider == LocationManager.GPS_PROVIDER) LocationSource.GPS else LocationSource.NETWORK,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy.toDouble(),
            elapsedRealtimeMillis = elapsedRealtimeMillis,
            isMock = isMock
        )
    }

    private companion object {
        val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        /** How long one check waits for a certain answer at most. */
        val LOCATION_WINDOW = 20.seconds

        const val SAMPLE_INTERVAL_MILLIS = 1_000L
    }
}
