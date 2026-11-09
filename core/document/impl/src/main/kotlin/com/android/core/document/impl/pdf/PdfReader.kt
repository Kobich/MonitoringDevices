package com.android.core.document.impl.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.LoadParams
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import com.android.core.document.api.DocumentFormat
import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import com.android.core.document.api.document.PageSize
import com.android.core.document.api.document.PdfDocument
import com.android.core.document.impl.FormatReader
import com.android.core.document.impl.mapReadErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Open questions: passwords need Android 15 (API 35), no text extraction.
 * Alternatives: androidx.pdf, pdfium.
 */
internal class PdfReader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FormatReader<PdfDocument> {
    override val formats = setOf(DocumentFormat.PDF)

    private val closeScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override suspend fun read(file: File, format: DocumentFormat, password: String?): PdfDocument {
        var opened: PdfDocument? = null
        try {
            return withContext(ioDispatcher) { open(file, password).also { document -> opened = document } }
        } catch (error: CancellationException) {
            // withContext drops a result that is ready when the caller is cancelled: close it here.
            opened?.close()
            throw error
        }
    }

    private fun open(file: File, password: String?): PdfDocument {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            return RendererPdfDocument(file.name, descriptor, openRenderer(descriptor, password))
        } catch (error: Throwable) {
            descriptor.close()
            throw error
        }
    }

    private fun openRenderer(descriptor: ParcelFileDescriptor, password: String?): PdfRenderer {
        try {
            return if (password != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                PdfRenderer(descriptor, LoadParams.Builder().setPassword(password).build())
            } else {
                PdfRenderer(descriptor)
            }
        } catch (error: SecurityException) {
            // Thrown for encrypted documents.
            throw when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM -> DocumentReadException(
                    Reason.UNSUPPORTED_FORMAT,
                    "Password-protected PDF files need Android 15 or newer",
                    error
                )

                password == null -> DocumentReadException(Reason.PASSWORD_REQUIRED, error.message, error)
                else -> DocumentReadException(Reason.INVALID_PASSWORD, error.message, error)
            }
        } catch (error: IOException) {
            throw DocumentReadException(Reason.CORRUPTED, error.message, error)
        }
    }

    /** `PdfRenderer` allows one open page at a time, so every call goes through [mutex]. */
    private inner class RendererPdfDocument(
        override val name: String,
        private val descriptor: ParcelFileDescriptor,
        private val renderer: PdfRenderer
    ) : PdfDocument {
        private val mutex = Mutex()
        private var isClosed = false

        override val format: DocumentFormat = DocumentFormat.PDF
        override val pageCount: Int = renderer.pageCount

        override suspend fun pageSize(pageIndex: Int): PageSize = withPage(pageIndex) { page ->
            // Broken files may report zero.
            PageSize(widthPt = page.width.coerceAtLeast(1), heightPt = page.height.coerceAtLeast(1))
        }

        override suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap = withPage(pageIndex) { page ->
            val width = widthPx.coerceIn(1, MAX_SIDE_PX)
            val height = (width.toLong() * page.height.coerceAtLeast(1) / page.width.coerceAtLeast(1))
                .toInt()
                .coerceIn(1, MAX_SIDE_PX)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }

        /** Returns at once; the renderer is released after a page that is rendering now. */
        override fun close() {
            synchronized(this) {
                if (isClosed) return
                isClosed = true
            }
            closeScope.launch {
                mutex.withLock {
                    renderer.close()
                    descriptor.close()
                }
            }
        }

        private suspend fun <T> withPage(pageIndex: Int, block: (PdfRenderer.Page) -> T): T {
            if (pageIndex !in 0 until pageCount) throw IndexOutOfBoundsException("Page $pageIndex of $pageCount")
            return withContext(ioDispatcher) {
                mutex.withLock {
                    if (synchronized(this@RendererPdfDocument) { isClosed }) {
                        throw DocumentReadException(Reason.IO_ERROR, "Document is closed")
                    }
                    mapReadErrors { renderer.openPage(pageIndex).use(block) }
                }
            }
        }
    }

    private companion object {
        /** Keeps a single page bitmap below ~64 MB. */
        const val MAX_SIDE_PX = 4096
    }
}
