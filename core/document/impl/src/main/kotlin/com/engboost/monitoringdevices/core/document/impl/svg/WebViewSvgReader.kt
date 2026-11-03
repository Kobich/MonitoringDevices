package com.engboost.monitoringdevices.core.document.impl.svg

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.document.HtmlPage
import com.engboost.monitoringdevices.core.document.api.format.image.SvgReader
import com.engboost.monitoringdevices.core.document.impl.cache.DiskRenderedDocumentCache
import com.engboost.monitoringdevices.core.document.impl.source.DocumentSourceFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * SVG has no platform decoder, but every WebView renders it. The file is shown through an
 * `<img>` tag in a small page, so scripts inside the SVG never run. Works on every API level.
 */
internal class WebViewSvgReader(
    private val cache: DiskRenderedDocumentCache,
    private val sourceFiles: DocumentSourceFiles,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SvgReader {

    override suspend fun read(source: DocumentSource, password: String?): HtmlDocument = withContext(ioDispatcher) {
        val identity = sourceFiles.identity(source)
        val entry = cache.find(identity, VARIANT) ?: try {
            cache.store(identity, VARIANT) { directory ->
                sourceFiles.openInputStream(source).use { input ->
                    File(directory, IMAGE_NAME).outputStream().use { output -> input.copyTo(output) }
                }
                val page = File(directory, PAGE_NAME).apply { writeText(PAGE_HTML) }
                listOf(HtmlPage(title = source.name, file = page))
            }
        } catch (error: DocumentReadException) {
            throw error
        } catch (error: IOException) {
            throw DocumentReadException(Reason.IO_ERROR, error.message, error)
        }

        HtmlDocument(
            name = source.name,
            format = DocumentFormat.SVG,
            directory = entry.directory,
            pages = entry.pages
        )
    }

    private companion object {
        const val VARIANT = "svg"
        const val IMAGE_NAME = "image.svg"
        const val PAGE_NAME = "index.html"
        const val PAGE_HTML = """<!DOCTYPE html>
<html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=10">
<style>
  html, body { margin: 0; height: 100%; background: #ffffff; }
  body { display: flex; align-items: center; justify-content: center; }
  img { max-width: 100%; max-height: 100%; }
</style></head>
<body><img src="image.svg" alt=""></body></html>
"""
    }
}
