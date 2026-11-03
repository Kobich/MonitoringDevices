package com.engboost.monitoringdevices.core.document.impl.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.LoadParams
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.document.PageSize
import com.engboost.monitoringdevices.core.document.api.document.PdfDocument
import com.engboost.monitoringdevices.core.document.api.format.pdf.PdfReader
import com.engboost.monitoringdevices.core.document.impl.source.DocumentSourceFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * PDF with the platform `PdfRenderer`: no dependencies, good quality.
 * Limits to decide on later: passwords need Android 15 (API 35), no text extraction here
 * (`pageText` returns `null`). Alternatives: androidx.pdf, pdfium.
 */
internal class AndroidPdfReader(
    private val sourceFiles: DocumentSourceFiles,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PdfReader {
    private val closeScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override suspend fun read(source: DocumentSource, password: String?): PdfDocument = withContext(ioDispatcher) {
        val descriptor = sourceFiles.openFileDescriptor(source)
        try {
            AndroidPdfDocument(source.name, descriptor, openRenderer(descriptor, password))
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
    private inner class AndroidPdfDocument(
        override val name: String,
        private val descriptor: ParcelFileDescriptor,
        private val renderer: PdfRenderer
    ) : PdfDocument {
        private val mutex = Mutex()
        private var isClosed = false

        override val format: DocumentFormat = DocumentFormat.PDF
        override val pageCount: Int = renderer.pageCount

        override suspend fun pageSize(pageIndex: Int): PageSize = withPage(pageIndex) { page ->
            // PdfRenderer reports sizes in points (1/72 inch); broken files may report zero.
            PageSize(widthPt = page.width.coerceAtLeast(1), heightPt = page.height.coerceAtLeast(1))
        }

        override suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap = withPage(pageIndex) { page ->
            val width = widthPx.coerceIn(1, MAX_SIDE_PX)
            val height = (width.toLong() * page.height.coerceAtLeast(1) / page.width.coerceAtLeast(1))
                .toInt()
                .coerceIn(1, MAX_SIDE_PX)
            val bitmap = try {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            } catch (error: OutOfMemoryError) {
                throw DocumentReadException(Reason.TOO_LARGE, "Not enough memory to render the page", error)
            }
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }

        override suspend fun pageText(pageIndex: Int): String? = null

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
                    if (synchronized(this@AndroidPdfDocument) { isClosed }) {
                        throw DocumentReadException(Reason.IO_ERROR, "Document is closed")
                    }
                    renderer.openPage(pageIndex).use(block)
                }
            }
        }
    }

    private companion object {
        /** Keeps a single page bitmap below ~64 MB. */
        const val MAX_SIDE_PX = 4096
    }
}
