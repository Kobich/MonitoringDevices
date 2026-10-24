package com.engboost.monitoringdevices.features.time

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
