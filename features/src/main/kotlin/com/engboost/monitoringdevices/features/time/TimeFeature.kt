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
import java.time.Instant

enum class TimeSource {
    /** Time synced from the internet (NTP). */
    NETWORK,

    /** Time from satellites (GNSS). */
    GNSS,

    /** System clock while automatic time is on. The user can still bypass it. */
    SYSTEM
}

data class TrustedTime(
    val instant: Instant,
    val source: TimeSource
)

/**
 * Current time from the most trusted source: network, then satellites, then the system clock
 * while automatic time is on. With manual time and no network or satellite time there is no
 * trusted time.
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

    override suspend fun getData(): TrustedTime? = readTime()

    override suspend fun isMeetsCondition(input: TimePeriod): ConditionResult {
        val time = readTime() ?: return ConditionResult.Unknown(UnknownReason.NOT_VERIFIED)
        return ConditionResult.of(time.instant in input)
    }

    private fun readTime(): TrustedTime? {
        readClock { SystemClock.currentNetworkTimeClock() }?.let { return TrustedTime(it, TimeSource.NETWORK) }
        readClock { SystemClock.currentGnssTimeClock() }?.let { return TrustedTime(it, TimeSource.GNSS) }
        val isAutoTime = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0) == 1
        return if (isAutoTime) TrustedTime(Instant.now(), TimeSource.SYSTEM) else null
    }

    /** The clock throws when its source has not been synced since boot. */
    private fun readClock(clock: () -> Clock): Instant? {
        return try {
            clock().instant()
        } catch (error: DateTimeException) {
            null
        }
    }
}
