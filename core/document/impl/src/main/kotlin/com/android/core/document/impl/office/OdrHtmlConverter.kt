package com.android.core.document.impl.office

import android.util.Log
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
     * [prepare] turns [file] into the file actually rendered (e.g. charts replaced by pictures) and may
     * write into the given work folder. [variant] is part of the cache key: different [prepare] steps
     * need different values.
     */
    suspend fun convert(
        file: File,
        format: DocumentFormat,
        variant: String,
        prepare: (file: File, workDirectory: File) -> File = { original, _ -> original }
    ): HtmlDocument = withContext(ioDispatcher) {
        // A ready document doesn't wait for another one being converted.
        val entry = cache.find(file, variant) ?: mutex.withLock {
            cache.find(file, variant) ?: run {
                Log.d(TAG, "convert: ${file.name} format=$format variant=$variant")
                val startedAt = System.currentTimeMillis()
                val workDirectory = cache.newWorkDirectory()
                try {
                    val input = prepare(file, workDirectory)
                    cache.store(file, variant) { outputDirectory -> render(input, outputDirectory) }
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

    private fun render(input: File, outputDirectory: File): List<HtmlPage> {
        try {
            Odr.open(input.path).use { decoded ->
                if (decoded.passwordEncrypted()) throw DocumentReadException(Reason.UNSUPPORTED_FORMAT, "Encrypted document")
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
                Html.translate(decoded, config).use { service ->
                    val views = service.listViews()
                    try {
                        val pages = service.bringOffline(outputDirectory.path, selectViews(views))
                            .pages()
                            .map { page ->
                                val file = File(page.path).let { file ->
                                    if (file.isAbsolute) file else File(outputDirectory, page.path)
                                }
                                HtmlPage(title = page.name, file = file)
                            }
                        val css = EXTRA_CSS + DocxFirstLineIndents.css(outputTexts(outputDirectory))
                        pages.forEach { page -> page.file.addStyle(css) }
                        return pages
                    } finally {
                        views.forEach { view -> runCatching { view.close() } }
                    }
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

    /** Pages and the scripts that fill the parts of a long document in later. */
    private fun outputTexts(outputDirectory: File): Sequence<String> {
        return outputDirectory.walk()
            .filter { file -> file.isFile && file.extension.lowercase() in setOf("html", "js") }
            .map { file -> file.readText() }
    }

    private fun File.addStyle(css: String) {
        val html = readText()
        val headEnd = html.indexOf("</head>")
        if (headEnd < 0) {
            Log.w(TAG, "addStyle: no </head> in $name")
            return
        }
        writeText(html.substring(0, headEnd) + "<style>$css</style>" + html.substring(headEnd))
    }

    private fun OdrException.toReadException(): DocumentReadException {
        val reason = when (this) {
            is OdrException.FileNotFound -> Reason.NOT_FOUND
            is OdrException.FileEncrypted,
            is OdrException.WrongPassword,
            is OdrException.DecryptionFailed,
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
        /**
         * A word longer than the line breaks like in Word instead of sticking out of the page; so does a
         * sentence glued with non-breaking spaces. The text box of a page is a flex item, which by default
         * grows to the longest word, so it needs min-width 0. In a table cell only `anywhere` breaks it:
         * `break-word` doesn't lower the width the table reserves for the column, and the column would
         * grow past its width in Word and stretch the page.
         */
        const val EXTRA_CSS = ".odr-page-inner{min-width:0}x-p{overflow-wrap:break-word}td x-p{overflow-wrap:anywhere}"

        // Default output names of HtmlConfig: document.html, sheet{index}.html, slide{index}.html, page{index}.html.
        const val DOCUMENT_FILE_PREFIX = "document"
        const val SHEET_FILE_PREFIX = "sheet"
    }
}
