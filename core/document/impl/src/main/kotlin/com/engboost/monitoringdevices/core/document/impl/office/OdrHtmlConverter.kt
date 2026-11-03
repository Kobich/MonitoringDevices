package com.engboost.monitoringdevices.core.document.impl.office

import app.opendocument.core.DecodedFile
import app.opendocument.core.Html
import app.opendocument.core.HtmlConfig
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
                    }
                    Html.translate(readable, config).use { service ->
                        return service.bringOffline(outputDirectory.path)
                            .pages()
                            .map { page ->
                                val file = File(page.path).let { file ->
                                    if (file.isAbsolute) file else File(outputDirectory, page.path)
                                }
                                HtmlPage(title = page.name, file = file)
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
}
