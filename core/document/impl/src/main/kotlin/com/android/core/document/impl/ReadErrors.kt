package com.android.core.document.impl

import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Only [DocumentReadException] may leave the module: other I/O errors, lack of memory and runtime errors of the parsers
 * are mapped to it. Cancellation passes through.
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
    } catch (error: CancellationException) {
        throw error
    } catch (error: SecurityException) {
        throw DocumentReadException(Reason.IO_ERROR, error.message, error)
    } catch (error: RuntimeException) {
        // PdfRenderer, the image decoders and the native OpenDocument.core report a damaged file this way.
        throw DocumentReadException(Reason.CORRUPTED, error.message ?: error::class.java.simpleName, error)
    }
}
