package com.android.core.document.impl.office

import android.util.Log
import app.opendocument.core.DecodedFile
import app.opendocument.core.Html
import app.opendocument.core.HtmlConfig
import app.opendocument.core.HtmlView
import app.opendocument.core.HtmlViewportMode
import app.opendocument.core.Odr
import app.opendocument.core.OdrException
import com.android.core.document.api.DocumentFormat
import com.android.core.document.api.DocumentReadException
import com.android.core.document.api.DocumentReadException.Reason
import com.android.core.document.api.document.HtmlDocument
import com.android.core.document.api.document.HtmlPage
import com.android.core.document.impl.cache.RenderCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "OdrHtmlConverter"

/** Native calls run one at a time until the library's thread safety is confirmed. */
internal class OdrHtmlConverter(
    private val cache: RenderCache,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val mutex = Mutex()

    /**
     * @param variant part of the cache key: different [prepare] steps need different values.
     * @param prepare turns [file] into the file actually rendered (e.g. charts replaced by
     * pictures); may write into the given work folder.
     */
    suspend fun convert(
        file: File,
        format: DocumentFormat,
        password: String?,
        variant: String,
        prepare: (file: File, workDirectory: File) -> File = { original, _ -> original }
    ): HtmlDocument = withContext(ioDispatcher) {
        // Cached under the password, so opening without it asks for the password again.
        // The key is hashed: the password isn't written anywhere.
        val cacheVariant = if (password == null) variant else "$variant|password:$password"

        // A ready document doesn't wait for another one being converted.
        val entry = cache.find(file, cacheVariant) ?: mutex.withLock {
            cache.find(file, cacheVariant) ?: run {
                Log.d(TAG, "convert: ${file.name} format=$format variant=$variant")
                val startedAt = System.currentTimeMillis()
                val workDirectory = cache.newWorkDirectory()
                try {
                    val input = prepare(file, workDirectory)
                    cache.store(file, cacheVariant) { outputDirectory -> render(input, password, outputDirectory) }
                        .also { stored ->
                            Log.d(TAG, "convert: ${file.name} done, pages=${stored.pages.size}, ${System.currentTimeMillis() - startedAt} ms")
                        }
                } finally {
                    workDirectory.deleteRecursively()
                }
            }
        }
        HtmlDocument(name = file.name, format = format, directory = entry.directory, pages = entry.pages)
    }

    private fun render(input: File, password: String?, outputDirectory: File): List<HtmlPage> {
        try {
            Odr.open(input.path).use { decoded ->
                val readable = decrypted(decoded, password)
                try {
                    val config = HtmlConfig().apply {
                        allowExternalContent = false
                        editable = false
                        // The viewer serves the output folder as a site: links to styles, scripts
                        // and images must be relative, not absolute file system paths.
                        relativeResourcePaths = true
                        // Word documents keep their pages, as in the original: without a page box the text
                        // reflows to the screen while pictures and tables keep their size and stick out.
                        textDocumentMargin = true
                        // Pages, slides and drawings open fitted to the screen width as a whole, so text, pictures
                        // and tables keep their proportions. FIT_WIDTH leaves the scale to the WebView (overview
                        // mode), which refits on rotation. Not FIT_WIDTH_BY_VIEW: its script measures once the page
                        // has loaded, and a WebView that gets its size after that opens zoomed in.
                        // Sheets have no page width: they stay at 100% and scroll.
                        viewportMode = HtmlViewportMode.FIT_WIDTH
                        spreadsheetViewportMode = HtmlViewportMode.ACTUAL_SIZE
                    }
                    Html.translate(readable, config).use { service ->
                        val views = service.listViews()
                        try {
                            return service.bringOffline(outputDirectory.path, selectViews(views))
                                .pages()
                                .map { page ->
                                    val file = File(page.path).let { file ->
                                        if (file.isAbsolute) file else File(outputDirectory, page.path)
                                    }
                                    HtmlPage(title = page.name, file = file)
                                }
                        } finally {
                            views.forEach { view -> runCatching { view.close() } }
                        }
                    }
                } finally {
                    if (readable !== decoded) readable.close()
                }
            }
        } catch (error: OdrException) {
            Log.w(TAG, "render: ${input.name} failed in OpenDocument.core: ${error::class.simpleName}", error)
            throw error.toReadException()
        }
    }

    /**
     * The library offers a view of the whole document (`document.html`) plus one view per
     * sheet, slide or page (`sheetN.html`, ...). Showing all of them duplicates content,
     * so spreadsheets get their sheets (shown as tabs) and everything else the whole document.
     */
    private fun selectViews(views: List<HtmlView>): List<HtmlView> {
        fun List<HtmlView>.withFilePrefix(prefix: String) =
            filter { view -> File(view.path()).name.startsWith(prefix) }

        val sheets = views.withFilePrefix(SHEET_FILE_PREFIX)
        if (sheets.isNotEmpty()) return sheets
        return views.withFilePrefix(DOCUMENT_FILE_PREFIX).ifEmpty { views }
    }

    private fun decrypted(decoded: DecodedFile, password: String?): DecodedFile {
        if (!decoded.passwordEncrypted()) return decoded
        if (password == null) throw DocumentReadException(Reason.PASSWORD_REQUIRED)
        return decoded.decrypt(password)
    }

    private fun OdrException.toReadException(): DocumentReadException {
        val reason = when (this) {
            is OdrException.FileNotFound -> Reason.NOT_FOUND
            is OdrException.FileEncrypted -> Reason.PASSWORD_REQUIRED
            is OdrException.WrongPassword,
            is OdrException.DecryptionFailed -> Reason.INVALID_PASSWORD

            is OdrException.UnknownFileType,
            is OdrException.UnsupportedFileType,
            is OdrException.UnknownDocumentType,
            is OdrException.NoDocumentFile,
            is OdrException.UnsupportedCryptoAlgorithm -> Reason.UNSUPPORTED_FORMAT

            is OdrException.FileReadError,
            is OdrException.FileWriteError -> Reason.IO_ERROR

            else -> Reason.CORRUPTED
        }
        return DocumentReadException(reason, message, this)
    }

    private companion object {
        // Default output names of HtmlConfig: document.html, sheet{index}.html, slide{index}.html, page{index}.html.
        const val DOCUMENT_FILE_PREFIX = "document"
        const val SHEET_FILE_PREFIX = "sheet"
    }
}
