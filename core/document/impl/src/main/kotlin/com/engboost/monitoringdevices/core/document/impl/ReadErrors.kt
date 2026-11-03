package com.engboost.monitoringdevices.core.document.impl

import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import java.io.IOException

/**
 * Keeps the API promise that only [DocumentReadException] leaves the module: other I/O errors
 * become [Reason.IO_ERROR], lack of memory becomes [Reason.TOO_LARGE].
 */
internal inline fun <T> mapReadErrors(block: () -> T): T {
    return try {
        block()
    } catch (error: DocumentReadException) {
        throw error
    } catch (error: IOException) {
        throw DocumentReadException(Reason.IO_ERROR, error.message, error)
    } catch (error: OutOfMemoryError) {
        throw DocumentReadException(Reason.TOO_LARGE, "Not enough memory", error)
    }
}
