package com.engboost.monitoringdevices.core.document.impl.office

import app.opendocument.core.DecodedFile
import app.opendocument.core.Html
import app.opendocument.core.HtmlConfig
import app.opendocument.core.HtmlView
import app.opendocument.core.Odr
import app.opendocument.core.OdrException
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.document.HtmlPage
import com.engboost.monitoringdevices.core.document.impl.cache.DiskRenderedDocumentCache
import com.engboost.monitoringdevices.core.document.impl.source.DocumentSourceFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The only class that talks to OpenDocument.core: office file -> HTML folder.
 * Native calls are serialized with a mutex until the library's thread safety is confirmed.
 */
internal class OdrHtmlConverter(
    private val cache: DiskRenderedDocumentCache,
    private val sourceFiles: DocumentSourceFiles,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val mutex = Mutex()

    /**
     * @param variant part of the cache key, different preprocessing must use different values.
     * @param prepare turns the source file into the file actually rendered (e.g. with charts
     * replaced by pictures); may write into the given work folder.
     */
    suspend fun convert(
        source: DocumentSource,
        format: DocumentFormat,
        password: String?,
        variant: String,
        prepare: (sourceFile: File, workDirectory: File) -> File = { sourceFile, _ -> sourceFile }
    ): HtmlDocument = withContext(ioDispatcher) {
        mutex.withLock {
            val identity = sourceFiles.identity(source)
            val entry = cache.find(identity, variant) ?: run {
                val workDirectory = cache.newWorkDirectory()
                try {
                    sourceFiles.withFile(source, workDirectory) { sourceFile ->
                        val input = prepare(sourceFile, workDirectory)
                        cache.store(identity, variant) { outputDirectory ->
                            render(input, password, outputDirectory)
                        }
                    }
                } finally {
                    workDirectory.deleteRecursively()
                }
            }

            HtmlDocument(
                name = source.name,
                format = format,
                directory = entry.directory,
                pages = entry.pages
            )
        }
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
