package com.android.core.document.impl

import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class ReadErrorsTest {

    @Test
    fun `errors of the parsers leave as DocumentReadException`() {
        assertEquals(Reason.IO_ERROR, reasonOf(IOException("disk")))
        assertEquals(Reason.IO_ERROR, reasonOf(SecurityException("no access")))
        assertEquals(Reason.TOO_LARGE, reasonOf(OutOfMemoryError()))
        assertEquals(Reason.CORRUPTED, reasonOf(IllegalStateException("PdfRenderer")))
        assertEquals(Reason.CORRUPTED, reasonOf(IllegalArgumentException()))
        assertEquals(Reason.NOT_FOUND, reasonOf(DocumentReadException(Reason.NOT_FOUND)))
    }

    @Test
    fun `cancellation passes through`() {
        assertThrows(CancellationException::class.java) { mapReadErrors { throw CancellationException("left") } }
    }

    private fun reasonOf(error: Throwable): Reason {
        return assertThrows(DocumentReadException::class.java) { mapReadErrors { throw error } }.reason
    }
}
