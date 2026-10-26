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
    SYSTEM,

    /** System clock set manually by the user: it can be anything. */
    MANUAL
}

data class CurrentTime(
    val instant: Instant,
    val source: TimeSource
)

/**
 * Current time from the most trusted source: network, then satellites, then the system clock
 * while automatic time is on, and as the last resort the system clock set manually.
 *
 * Network and satellite time keep counting from their last sync with an internal timer,
 * so changing the clock in the settings does not affect them. They are lost after a reboot
 * until the next sync.
 *
 * [getData] always returns the current time and its source; check the source to see how much to trust it.
 * [isMeetsCondition] checks whether the current time is inside the given period. Manual time
 * outside the period gives false; manual time inside it is not trusted.
 *
 * Unknown reasons: [UnknownReason.NOT_VERIFIED] (only manual time is available and it is inside the period).
 */
class TimeFeature(context: Context) :
    DataFeature<CurrentTime>,
    ConditionFeature<TimePeriod> {

    private val contentResolver = context.applicationContext.contentResolver

    override suspend fun getData(): CurrentTime = readTime()

    override suspend fun isMeetsCondition(input: TimePeriod): ConditionResult {
        val time = readTime()
        val isInside = time.instant in input
        if (isInside && time.source == TimeSource.MANUAL) return ConditionResult.Unknown(UnknownReason.NOT_VERIFIED)
        return ConditionResult.of(isInside)
    }

    private fun readTime(): CurrentTime {
        readClock { SystemClock.currentNetworkTimeClock() }?.let { return CurrentTime(it, TimeSource.NETWORK) }
        readClock { SystemClock.currentGnssTimeClock() }?.let { return CurrentTime(it, TimeSource.GNSS) }
        val isAutoTime = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0) == 1
        return CurrentTime(Instant.now(), if (isAutoTime) TimeSource.SYSTEM else TimeSource.MANUAL)
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
