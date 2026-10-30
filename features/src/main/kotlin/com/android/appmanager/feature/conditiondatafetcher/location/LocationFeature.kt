package com.android.appmanager.feature.conditiondatafetcher.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.SystemClock
import com.android.appmanager.feature.conditiondatafetcher.ConditionFeature
import com.android.appmanager.feature.conditiondatafetcher.ConditionResult
import com.android.appmanager.feature.conditiondatafetcher.DataFeature
import com.android.appmanager.feature.conditiondatafetcher.GeoCondition
import com.android.appmanager.feature.conditiondatafetcher.GeoData
import com.android.appmanager.feature.conditiondatafetcher.SourceId
import com.android.appmanager.feature.conditiondatafetcher.UnknownReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Listens to fused, GPS and network location at once, starting from their last known fixes, for up
 * to [window]; returns as soon as the answer is clear. Rules are in [LocationJudge]. No spoofing protection.
 *
 * Without a SIM card and internet the first GPS fix can take minutes, longer than the default window.
 * In the background it needs a foreground service of type `location`.
 *
 * [trace] gets a readable line per step (sources, satellites, fixes, result) for logs and debugging.
 */
@SuppressLint("MissingPermission")
class LocationFeature(
    context: Context,
    private val window: Duration = DEFAULT_WINDOW,
    private val trace: (String) -> Unit = {}
) : DataFeature<GeoData>,
    ConditionFeature<GeoCondition> {

    override fun getId() = SourceId.GEO

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    override suspend fun getData(): GeoData? {
        val startMillis = SystemClock.elapsedRealtime()
        val judge = LocationJudge(zones = emptyList(), trace = trace)
        val failure = listen(judge) { nowMillis ->
            val best = judge.bestSample(nowMillis)
            best != null && nowMillis - best.elapsedRealtimeMillis <= FRESH_SAMPLE_AGE_MILLIS
        }
        val nowMillis = SystemClock.elapsedRealtime()
        val result = if (failure == null) judge.bestSample(nowMillis)?.toGeoData(nowMillis) else null
        trace("Result: ${result ?: "null ($failure)"} after ${nowMillis - startMillis} ms")
        return result
    }

    override suspend fun isMeetsCondition(input: GeoCondition): ConditionResult {
        if (input.zones.isEmpty()) return ConditionResult.False

        val startMillis = SystemClock.elapsedRealtime()
        val judge = LocationJudge(zones = input.zones, trace = trace)
        var certain: ConditionResult? = null
        val failure = listen(judge) { nowMillis ->
            certain = judge.certainResult(nowMillis)
            certain != null
        }
        val nowMillis = SystemClock.elapsedRealtime()
        val certainResult = certain
        val result = when {
            failure != null -> ConditionResult.Unknown(failure)
            certainResult != null -> certainResult
            else -> {
                trace("Window is over without a certain answer")
                judge.finalResult(nowMillis)
            }
        }
        trace("Result: $result after ${nowMillis - startMillis} ms")
        return result
    }

    /** Returns null when listening ran, otherwise why it could not. */
    private suspend fun listen(judge: LocationJudge, stopWhen: (nowMillis: Long) -> Boolean): UnknownReason? {
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            trace("ACCESS_FINE_LOCATION is not granted")
            return UnknownReason.PERMISSION_DENIED
        }
        val manager = locationManager ?: return UnknownReason.SOURCE_UNAVAILABLE
        if (!manager.isLocationEnabled) {
            trace("Location is turned off")
            return UnknownReason.SOURCE_DISABLED
        }

        val states = PROVIDERS.associateWith { manager.providerState(it) }
        trace("Sources: " + states.entries.joinToString { (provider, state) -> "${provider.uppercase()} $state" })
        val providers = PROVIDERS.filter { states[it] == ProviderState.ENABLED }
        if (providers.isEmpty()) {
            return if (states.values.all { it == ProviderState.MISSING }) {
                UnknownReason.SOURCE_UNAVAILABLE
            } else {
                UnknownReason.SOURCE_DISABLED
            }
        }

        val samples = Channel<LocationSample>(Channel.UNLIMITED)
        val listeners = providers.associateWith { provider ->
            LocationListener { location -> location.toSample(provider)?.let { samples.trySend(it) } }
        }
        val request = LocationRequest.Builder(SAMPLE_INTERVAL_MILLIS)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()
        val gnssCallback = SatelliteTrace().takeIf { LocationManager.GPS_PROVIDER in providers }

        return try {
            // A recent last known fix can answer without waiting.
            trace("Last known fixes:")
            providers.forEach { provider ->
                val sample = manager.getLastKnownLocation(provider)?.toSample(provider)
                if (sample == null) {
                    trace("${provider.uppercase()}: none")
                } else {
                    judge.addSample(sample, SystemClock.elapsedRealtime())
                }
            }
            if (stopWhen(SystemClock.elapsedRealtime())) return null

            listeners.forEach { (provider, listener) ->
                manager.requestLocationUpdates(provider, request, appContext.mainExecutor, listener)
            }
            gnssCallback?.let {
                val isRegistered = manager.registerGnssStatusCallback(appContext.mainExecutor, it)
                trace(if (isRegistered) "Satellite status: subscribed" else "Satellite status: subscription refused")
            }
            trace("Listening to ${providers.joinToString { it.uppercase() }} for up to $window")
            withTimeoutOrNull(window) {
                for (sample in samples) {
                    judge.addSample(sample, SystemClock.elapsedRealtime())
                    if (stopWhen(SystemClock.elapsedRealtime())) break
                }
            }
            null
        } catch (error: SecurityException) {
            UnknownReason.PERMISSION_DENIED
        } finally {
            listeners.values.forEach { runCatching { manager.removeUpdates(it) } }
            gnssCallback?.let { runCatching { manager.unregisterGnssStatusCallback(it) } }
        }
    }

    private inner class SatelliteTrace : GnssStatus.Callback() {
        private var lastText: String? = null

        override fun onStarted() {
            trace("GPS engine started")
        }

        override fun onStopped() {
            trace("GPS engine stopped")
        }

        override fun onFirstFix(ttffMillis: Int) {
            trace("GPS first fix after $ttffMillis ms")
        }

        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val indices = 0 until status.satelliteCount
            val withSignal = indices.count { status.getCn0DbHz(it) > 0 }
            val usedInFix = indices.count { status.usedInFix(it) }
            val text = "Satellites: ${status.satelliteCount} visible, $withSignal with signal, $usedInFix used in fix"
            if (text != lastText) {
                lastText = text
                trace(text)
            }
        }
    }

    private fun LocationManager.providerState(provider: String): ProviderState = when {
        !hasProvider(provider) -> ProviderState.MISSING
        !isProviderEnabled(provider) -> ProviderState.DISABLED
        else -> ProviderState.ENABLED
    }

    private enum class ProviderState { MISSING, DISABLED, ENABLED }

    private fun Location.toSample(provider: String): LocationSample? {
        if (!hasAccuracy()) {
            trace("${provider.uppercase()} fix dropped: no accuracy")
            return null
        }
        return LocationSample(
            source = when (provider) {
                LocationManager.GPS_PROVIDER -> LocationSource.GPS
                LocationManager.NETWORK_PROVIDER -> LocationSource.NETWORK
                else -> LocationSource.FUSED
            },
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy.toDouble(),
            elapsedRealtimeMillis = elapsedRealtimeMillis
        )
    }

    private fun LocationSample.toGeoData(nowMillis: Long) = GeoData(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        source = source,
        ageMillis = nowMillis - elapsedRealtimeMillis
    )

    private companion object {
        val PROVIDERS = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        val DEFAULT_WINDOW = 20.seconds

        const val FRESH_SAMPLE_AGE_MILLIS = 10_000L

        const val SAMPLE_INTERVAL_MILLIS = 1_000L
    }
}
