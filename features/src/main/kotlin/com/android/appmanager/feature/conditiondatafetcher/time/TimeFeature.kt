package com.android.appmanager.feature.conditiondatafetcher.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.android.appmanager.feature.conditiondatafetcher.ConditionFeature
import com.android.appmanager.feature.conditiondatafetcher.ConditionResult
import com.android.appmanager.feature.conditiondatafetcher.DataFeature
import com.android.appmanager.feature.conditiondatafetcher.SourceId
import com.android.appmanager.feature.conditiondatafetcher.TimeCondition
import com.android.appmanager.feature.conditiondatafetcher.TimeData
import com.android.appmanager.feature.conditiondatafetcher.UnknownReason
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant

/**
 * Sources in order of trust: network, satellites, system clock with automatic time, manual clock.
 * Network and satellite time count from their last sync, so changing the clock does not affect them;
 * after a reboot they are gone until the next sync.
 *
 * Manual time outside every period gives false, inside any of them gives [UnknownReason.NOT_VERIFIED].
 */
class TimeFeature(context: Context) :
    DataFeature<TimeData>,
    ConditionFeature<TimeCondition> {

    override fun getId() = SourceId.TIME

    private val contentResolver = context.applicationContext.contentResolver

    override suspend fun getData(): TimeData = readTime()

    override suspend fun isMeetsCondition(input: TimeCondition): ConditionResult {
        if (input.periods.isEmpty()) return ConditionResult.False

        val time = readTime()
        val isInside = input.periods.any { time.instant in it }
        if (isInside && time.source == TimeSource.MANUAL) return ConditionResult.Unknown(UnknownReason.NOT_VERIFIED)
        return ConditionResult.of(isInside)
    }

    private fun readTime(): TimeData {
        readClock { SystemClock.currentNetworkTimeClock() }?.let { return TimeData(it, TimeSource.NETWORK) }
        readClock { SystemClock.currentGnssTimeClock() }?.let { return TimeData(it, TimeSource.GNSS) }
        val isAutoTime = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0) == 1
        return TimeData(Instant.now(), if (isAutoTime) TimeSource.SYSTEM else TimeSource.MANUAL)
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
