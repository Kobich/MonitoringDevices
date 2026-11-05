package com.engboost.monitoringdevices.core.document.impl

import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import java.io.IOException

/** Only [DocumentReadException] may leave the module: other I/O errors and lack of memory are mapped to it. */
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
