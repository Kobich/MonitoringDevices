package com.android.appmanager.feature.documentviewer.presentation.screen

import kotlin.coroutines.cancellation.CancellationException

/**
 * [runCatching] that lets cancellation through. In `produceState` a cancelled load would otherwise set its failure
 * after the restarted one (a new key) had already shown the result.
 */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
