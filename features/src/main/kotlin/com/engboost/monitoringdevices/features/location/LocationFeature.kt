package com.engboost.monitoringdevices.features.location

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
import com.engboost.monitoringdevices.features.ConditionFeature
import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.DataFeature
import com.engboost.monitoringdevices.features.UnknownReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Current location and whether the user is inside any of the given zones.
 *
 * Uses every enabled source: Android's combined location (fused), GPS and network location
 * (Wi-Fi and cell towers). The last known fix of each source is used at once, then new fixes
 * are awaited for up to [window]. Without a SIM card and internet the first GPS fix outdoors can
 * take minutes, longer than the default 20 seconds. The most accurate fix wins; how fixes are judged is
 * described in [LocationJudge]. There is no spoofing protection.
 *
 * [getData] returns as soon as a fix is at most [FRESH_SAMPLE_AGE_MILLIS] old, otherwise the best
 * fix when the window is over, or null if there is none.
 * [isMeetsCondition] returns as soon as the best fix is certain.
 *
 * Unknown reasons: [UnknownReason.PERMISSION_DENIED], [UnknownReason.SOURCE_UNAVAILABLE],
 * [UnknownReason.SOURCE_DISABLED], [UnknownReason.NO_DATA], [UnknownReason.NOT_ACCURATE_ENOUGH].
 *
 * [trace] receives a readable line for every step: which sources are available, how many satellites
 * GPS sees and uses, every fix and the result. Use it for logs and debugging.
 *
 * In the background it needs a foreground service of type `location`.
 */
@SuppressLint("MissingPermission")
class LocationFeature(
    context: Context,
    private val window: Duration = DEFAULT_WINDOW,
    private val trace: (String) -> Unit = {}
) : DataFeature<CurrentLocation>,
    ConditionFeature<List<GeoZone>> {

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    override suspend fun getData(): CurrentLocation? {
        val startMillis = SystemClock.elapsedRealtime()
        val judge = LocationJudge(zones = emptyList(), trace = trace)
        val failure = listen(judge) { nowMillis ->
            val best = judge.bestSample(nowMillis)
            best != null && nowMillis - best.elapsedRealtimeMillis <= FRESH_SAMPLE_AGE_MILLIS
        }
        val nowMillis = SystemClock.elapsedRealtime()
        val result = if (failure == null) judge.bestSample(nowMillis)?.toCurrentLocation(nowMillis) else null
        trace("Result: ${result ?: "null ($failure)"} after ${nowMillis - startMillis} ms")
        return result
    }

    override suspend fun isMeetsCondition(input: List<GeoZone>): ConditionResult {
        if (input.isEmpty()) return ConditionResult.False

        val startMillis = SystemClock.elapsedRealtime()
        val judge = LocationJudge(zones = input, trace = trace)
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

    /**
     * Feeds the last known fix and then new fixes of every enabled source into [judge]
     * until [stopWhen] returns true or [window] passes.
     * Returns null when listening ran, or the reason it could not run.
     */
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
            // The last known fixes come at once, so a recent one can answer without waiting.
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

    /** Traces how many satellites GPS sees and uses, only when the numbers change. */
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

    private fun LocationSample.toCurrentLocation(nowMillis: Long) = CurrentLocation(
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

        /** How long one call waits for new fixes at most by default. */
        val DEFAULT_WINDOW = 20.seconds

        /** [getData] returns as soon as it has a fix at most this old. */
        const val FRESH_SAMPLE_AGE_MILLIS = 10_000L

        const val SAMPLE_INTERVAL_MILLIS = 1_000L
    }
}
