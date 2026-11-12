package com.android.core.logger.client.api

import android.util.Log

private const val TAG = "ActionLogger"

/**
 * Stub with the API of the real logger client: events only go to Logcat. Replace this module with the
 * real one; the package and the names match, so callers don't change.
 */
object ActionLogger {
    fun logUi(event: UiInteractionEvent) {
        Log.d(TAG, "ui: $event")
    }
}
