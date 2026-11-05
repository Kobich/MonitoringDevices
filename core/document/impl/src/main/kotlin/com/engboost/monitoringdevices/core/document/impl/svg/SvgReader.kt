package com.engboost.monitoringdevices.core.document.impl.svg

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.document.HtmlPage
import com.engboost.monitoringdevices.core.document.impl.FormatReader
import com.engboost.monitoringdevices.core.document.impl.cache.RenderCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The SVG is shown through `<img>`, so its scripts never run. */
internal class SvgReader(
    private val cache: RenderCache,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FormatReader<HtmlDocument> {
    override val formats = setOf(DocumentFormat.SVG)

    override suspend fun read(file: File, format: DocumentFormat, password: String?): HtmlDocument = withContext(ioDispatcher) {
        val entry = cache.find(file, VARIANT) ?: cache.store(file, VARIANT) { directory ->
            file.copyTo(File(directory, IMAGE_NAME))
            val page = File(directory, PAGE_NAME).apply { writeText(PAGE_HTML) }
            listOf(HtmlPage(title = file.name, file = page))
        }
        HtmlDocument(name = file.name, format = format, directory = entry.directory, pages = entry.pages)
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
