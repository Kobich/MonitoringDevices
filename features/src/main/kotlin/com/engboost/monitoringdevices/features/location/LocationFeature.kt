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
 * Network location is required: GPS alone is never trusted, so without network location GPS is
 * not turned on at all and the result is [UnknownReason.SOURCE_UNAVAILABLE] or
 * [UnknownReason.SOURCE_DISABLED].
 *
 * [trace] receives a readable line for every step: which sources are available, every received
 * fix and whether it was used, and the result. Use it for logs and debugging.
 *
 * In the background it needs a foreground service of type `location`.
 */
@SuppressLint("MissingPermission")
class LocationFeature(
    context: Context,
    private val trace: (String) -> Unit = {}
) : ConditionFeature<List<GeoZone>> {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    override suspend fun isMeetsCondition(input: List<GeoZone>): ConditionResult {
        val startMillis = SystemClock.elapsedRealtime()
        val result = check(input)
        trace("Result: $result after ${SystemClock.elapsedRealtime() - startMillis} ms")
        return result
    }

    private suspend fun check(zones: List<GeoZone>): ConditionResult {
        if (zones.isEmpty()) return ConditionResult.False
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            trace("ACCESS_FINE_LOCATION is not granted")
            return ConditionResult.Unknown(UnknownReason.PERMISSION_DENIED)
        }
        val manager = locationManager ?: return ConditionResult.Unknown(UnknownReason.SOURCE_UNAVAILABLE)
        if (!manager.isLocationEnabled) {
            trace("Location is turned off")
            return ConditionResult.Unknown(UnknownReason.SOURCE_DISABLED)
        }

        val gpsState = manager.providerState(LocationManager.GPS_PROVIDER)
        val networkState = manager.providerState(LocationManager.NETWORK_PROVIDER)
        trace("Sources: GPS $gpsState, network $networkState")
        when (networkState) {
            ProviderState.MISSING -> {
                trace("No network location on this device, GPS alone is not trusted, GPS is not turned on")
                return ConditionResult.Unknown(UnknownReason.SOURCE_UNAVAILABLE)
            }

            ProviderState.DISABLED -> {
                trace("Network location is turned off (on Google devices: Google Location Accuracy), GPS is not turned on")
                return ConditionResult.Unknown(UnknownReason.SOURCE_DISABLED)
            }

            ProviderState.ENABLED -> Unit
        }

        val isGpsExpected = gpsState == ProviderState.ENABLED
        val providers = if (isGpsExpected) PROVIDERS else listOf(LocationManager.NETWORK_PROVIDER)
        val judge = LocationJudge(zones, isGpsExpected, trace)
        val samples = Channel<LocationSample>(Channel.UNLIMITED)
        val listeners = providers.associateWith { provider ->
            LocationListener { location ->
                val sample = location.toSample(provider)
                if (sample == null) {
                    trace("${provider.uppercase()} fix dropped: no accuracy")
                } else {
                    samples.trySend(sample)
                }
            }
        }
        val request = LocationRequest.Builder(SAMPLE_INTERVAL_MILLIS)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()

        return try {
            listeners.forEach { (provider, listener) ->
                manager.requestLocationUpdates(provider, request, appContext.mainExecutor, listener)
            }
            trace("Listening to ${providers.joinToString { it.uppercase() }} for up to $LOCATION_WINDOW")
            val certainResult = withTimeoutOrNull(LOCATION_WINDOW) {
                for (sample in samples) {
                    judge.addSample(sample, SystemClock.elapsedRealtime())?.let { return@withTimeoutOrNull it }
                }
                null
            }
            certainResult ?: run {
                trace("Window is over without a certain answer")
                judge.finalResult(SystemClock.elapsedRealtime())
            }
        } catch (error: SecurityException) {
            ConditionResult.Unknown(UnknownReason.PERMISSION_DENIED)
        } finally {
            listeners.values.forEach { runCatching { manager.removeUpdates(it) } }
        }
    }

    private fun LocationManager.providerState(provider: String): ProviderState = when {
        !hasProvider(provider) -> ProviderState.MISSING
        !isProviderEnabled(provider) -> ProviderState.DISABLED
        else -> ProviderState.ENABLED
    }

    private enum class ProviderState { MISSING, DISABLED, ENABLED }

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
