package com.engboost.monitoringdevices.scanner.bluetooth

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps scan starts at least [minIntervalMillis] apart.
 * A start requested too early is delayed, not dropped.
 */
internal class ScanStartLimiter(private val minIntervalMillis: Long) {
    private val mutex = Mutex()
    private var lastStartMillis: Long? = null

    /** Suspends until a new scan may be started and reserves that start. */
    suspend fun awaitStart() {
        mutex.withLock {
            lastStartMillis?.let { lastStart ->
                val waitMillis = lastStart + minIntervalMillis - SystemClock.elapsedRealtime()
                if (waitMillis > 0) delay(waitMillis)
            }
            lastStartMillis = SystemClock.elapsedRealtime()
        }
    }
}
