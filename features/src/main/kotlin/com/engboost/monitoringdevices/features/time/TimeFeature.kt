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
    NETWORK,

    GNSS,

    /** System clock with automatic time on. Still possible to bypass. */
    SYSTEM,

    MANUAL
}

data class CurrentTime(
    val instant: Instant,
    val source: TimeSource
)

/**
 * Sources in order of trust: network, satellites, system clock with automatic time, manual clock.
 * Network and satellite time count from their last sync, so changing the clock does not affect them;
 * after a reboot they are gone until the next sync.
 *
 * Manual time outside the period gives false, inside it gives [UnknownReason.NOT_VERIFIED].
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

    /** Throws until its source has synced since boot. */
    private fun readClock(clock: () -> Clock): Instant? {
        return try {
            clock().instant()
        } catch (error: DateTimeException) {
            null
        }
    }
}
