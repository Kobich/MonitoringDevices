package com.android.appmanager.feature.conditiondatafetcher

import com.android.appmanager.feature.conditiondatafetcher.location.GeoZone
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Everything [ConditionFeature.isMeetsCondition] can take. Kotlin requires all of them in this package.
 * A condition with a list is met when any item matches; an empty list gives false.
 */
sealed interface ConditionInput

data class TimeCondition(val periods: List<TimePeriod>) : ConditionInput

/**
 * [start] inclusive, [end] exclusive. ISO 8601 with an offset, for example `2026-10-24T09:00:00+03:00`;
 * the phone's time zone is never used.
 */
data class TimePeriod(
    val start: String,
    val end: String
) {
    private val startInstant = parse(start, "start")
    private val endInstant = parse(end, "end")

    init {
        require(endInstant.isAfter(startInstant)) { "end must be after start" }
    }

    operator fun contains(instant: Instant): Boolean {
        return !instant.isBefore(startInstant) && instant.isBefore(endInstant)
    }

    private companion object {
        fun parse(value: String, name: String): Instant {
            return try {
                ZonedDateTime.parse(value).toInstant()
            } catch (error: DateTimeParseException) {
                throw IllegalArgumentException(
                    "$name must be ISO 8601 with a time zone offset, for example 2026-10-24T09:00:00+03:00: $value",
                    error
                )
            }
        }
    }
}

data class GeoCondition(val zones: List<GeoZone>) : ConditionInput

data class WifiFoundCondition(val networks: List<WifiNetworkCondition>) : ConditionInput

/** [name] must match; [bssid] is checked only when set. */
data class WifiNetworkCondition(
    val name: String,
    val bssid: String? = null
)

data class BtFoundCondition(val names: List<String>) : ConditionInput

data class BtConnectedCondition(val names: List<String>) : ConditionInput
