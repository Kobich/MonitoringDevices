package com.engboost.monitoringdevices.features.time

import java.time.Instant
import java.time.ZonedDateTime

/** Time period from [start] inclusive to [end] exclusive. Time zones come with the values. */
data class TimePeriod(
    val start: ZonedDateTime,
    val end: ZonedDateTime
) {
    init {
        require(end.isAfter(start)) { "end must be after start" }
    }

    operator fun contains(instant: Instant): Boolean {
        return !instant.isBefore(start.toInstant()) && instant.isBefore(end.toInstant())
    }
}
