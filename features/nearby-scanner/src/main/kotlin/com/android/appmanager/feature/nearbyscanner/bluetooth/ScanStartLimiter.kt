package com.android.appmanager.feature.nearbyscanner.bluetooth

import android.os.SystemClock

internal class ScanStartLimiter(private val minIntervalMillis: Long) {
    private var lastStartMillis: Long? = null

    @Synchronized
    fun tryStart(): Boolean {
        val nowMillis = SystemClock.elapsedRealtime()
        val lastStart = lastStartMillis
        if (lastStart != null && nowMillis - lastStart < minIntervalMillis) return false

        lastStartMillis = nowMillis
        return true
    }
}
