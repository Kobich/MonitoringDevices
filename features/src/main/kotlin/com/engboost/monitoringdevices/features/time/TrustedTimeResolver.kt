package com.engboost.monitoringdevices.features.time

import java.time.Instant

/**
 * Picks the most trusted current time. Plain Kotlin without Android, so the order can be unit tested.
 *
 * Order: network time, then satellite time, then system time. System time is used only while
 * automatic time is on; with manual time the user can set any value, so there is no trusted time.
 */
internal class TrustedTimeResolver(
    private val networkTimeMillis: () -> Long?,
    private val gnssTimeMillis: () -> Long?,
    private val systemTimeMillis: () -> Long,
    private val isAutoTimeEnabled: () -> Boolean
) {
    /** The current time and where it came from, or null when no source can be trusted. */
    fun resolve(): TrustedTime? {
        networkTimeMillis()?.let { return TrustedTime(Instant.ofEpochMilli(it), TimeSource.NETWORK) }
        gnssTimeMillis()?.let { return TrustedTime(Instant.ofEpochMilli(it), TimeSource.GNSS) }
        if (isAutoTimeEnabled()) return TrustedTime(Instant.ofEpochMilli(systemTimeMillis()), TimeSource.SYSTEM)
        return null
    }
}
