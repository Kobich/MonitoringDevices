package com.engboost.monitoringdevices.features.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.engboost.monitoringdevices.features.ConditionFeature
import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.DataFeature
import com.engboost.monitoringdevices.features.UnknownReason
import java.time.Clock
import java.time.DateTimeException

/**
 * Current time from the most trusted source: network, then satellites, then the system clock
 * while automatic time is on. See [TrustedTimeResolver].
 *
 * Network and satellite time keep counting from their last sync with an internal timer,
 * so changing the clock in the settings does not affect them. They are lost after a reboot
 * until the next sync.
 *
 * [getData] returns the current time and its source, or null when no source can be trusted.
 * [isMeetsCondition] checks whether the current time is inside the given period.
 *
 * Unknown reasons: [UnknownReason.NOT_VERIFIED] (no network or satellite time and the time is set manually).
 */
class TimeFeature(context: Context) :
    DataFeature<TrustedTime>,
    ConditionFeature<TimePeriod> {

    private val contentResolver = context.applicationContext.contentResolver
    private val resolver = TrustedTimeResolver(
        networkTimeMillis = { readMillis { SystemClock.currentNetworkTimeClock() } },
        gnssTimeMillis = { readMillis { SystemClock.currentGnssTimeClock() } },
        systemTimeMillis = System::currentTimeMillis,
        isAutoTimeEnabled = { Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0) == 1 }
    )

    override suspend fun getData(): TrustedTime? = resolver.resolve()

    override suspend fun isMeetsCondition(input: TimePeriod): ConditionResult {
        val time = resolver.resolve() ?: return ConditionResult.Unknown(UnknownReason.NOT_VERIFIED)
        return ConditionResult.of(time.instant in input)
    }

    /** The clock throws when its source has not been synced since boot. */
    private fun readMillis(clock: () -> Clock): Long? {
        return try {
            clock().millis()
        } catch (error: DateTimeException) {
            null
        }
    }
}
