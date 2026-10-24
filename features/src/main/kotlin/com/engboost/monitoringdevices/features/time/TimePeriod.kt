package com.engboost.monitoringdevices.features.time

import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Time period from [start] inclusive to [end] exclusive.
 *
 * Both are ISO 8601 strings with a time zone offset, for example `2026-10-24T09:00:00+03:00`,
 * `2026-10-24T06:00:00Z` or `2026-10-24T09:00:00+03:00[Europe/Moscow]`.
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
