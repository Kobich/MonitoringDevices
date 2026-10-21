package com.engboost.monitoringdevices.scanner.bluetooth

import android.os.SystemClock

/** Allows scan starts no more often than once per [minIntervalMillis]. */
internal class ScanStartLimiter(private val minIntervalMillis: Long) {
    private var lastStartMillis: Long? = null

    /** Reserves a start and returns true, or returns false if the previous start was too recent. */
    @Synchronized
    fun tryStart(): Boolean {
        val nowMillis = SystemClock.elapsedRealtime()
        val lastStart = lastStartMillis
        if (lastStart != null && nowMillis - lastStart < minIntervalMillis) return false

        lastStartMillis = nowMillis
        return true
    }
}
