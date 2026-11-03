package com.engboost.monitoringdevices.core.document.impl

import android.content.Context
import android.os.Build
import com.engboost.monitoringdevices.core.document.api.DocumentReader
import com.engboost.monitoringdevices.core.document.api.cache.RenderedDocumentCache
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.format.FormatReader
import com.engboost.monitoringdevices.core.document.impl.audio.AndroidAudioReader
import com.engboost.monitoringdevices.core.document.impl.cache.DiskRenderedDocumentCache
import com.engboost.monitoringdevices.core.document.impl.chart.AndroidChartImageRenderer
import com.engboost.monitoringdevices.core.document.impl.chart.DocxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.chart.PptxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.image.AndroidImageReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.drawing.OdrOdgReader
import com.engboost.monitoringdevices.core.document.impl.office.presentation.OdrKeynoteReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrNumbersReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrPagesReader
import com.engboost.monitoringdevices.core.document.impl.svg.WebViewSvgReader
import com.engboost.monitoringdevices.core.document.impl.text.AndroidMarkdownReader
import com.engboost.monitoringdevices.core.document.impl.office.presentation.OdrOdpReader
import com.engboost.monitoringdevices.core.document.impl.office.presentation.OdrPptReader
import com.engboost.monitoringdevices.core.document.impl.office.presentation.OdrPptxReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrCsvReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrOdsReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrXlsReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrXlsxReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrDocReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrDocxReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrOdtReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrRtfReader
import com.engboost.monitoringdevices.core.document.impl.pdf.AndroidPdfReader
import com.engboost.monitoringdevices.core.document.impl.source.DocumentSourceFiles
import com.engboost.monitoringdevices.core.document.impl.text.AndroidTextReader
import java.io.File

/**
 * The only public entry point of the module: builds all readers by hand, without
 * a DI framework, so any app (Hilt, Koin, manual) can use it. Create one instance per process,
 * e.g. as a Hilt `@Singleton`.
 *
 * ```
 * val core = DocumentCore(context)
 * val document = core.reader.read("/path/to/report.docx")
 * ```
 */
class DocumentCore(
    context: Context,
    config: DocumentCoreConfig = DocumentCoreConfig()
) {
    private val appContext = context.applicationContext
    private val diskCache = DiskRenderedDocumentCache(File(appContext.cacheDir, config.cacheDirectoryName))
    private val sourceFiles = DocumentSourceFiles(appContext)

    /**
     * OpenDocument.core needs API 26: on older devices its classes are never loaded, office
     * formats are unsupported and Markdown is shown as plain text.
     */
    private val officeConverter: OdrHtmlConverter? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) OdrHtmlConverter(diskCache, sourceFiles) else null

    /** On-disk output of converted documents (office files to HTML). */
    val cache: RenderedDocumentCache = diskCache

    /** Opens any supported file: `reader.read(path)`. */
    val reader: DocumentReader = CompositeDocumentReader(platformReaders(config) + officeReaders(), sourceFiles)

    /** PDF, text, images, SVG and audio on platform APIs, available on every API level. */
    private fun platformReaders(config: DocumentCoreConfig): List<FormatReader<Document>> {
        val textReader = AndroidTextReader(sourceFiles, maxChars = config.maxTextChars)
        return listOf(
            AndroidPdfReader(sourceFiles),
            textReader,
            AndroidMarkdownReader(officeConverter, textReader),
            AndroidImageReader(sourceFiles),
            WebViewSvgReader(diskCache, sourceFiles),
            AndroidAudioReader(appContext)
        )
    }

    /** Office formats via OpenDocument.core, API 26+. */
    private fun officeReaders(): List<FormatReader<Document>> {
        val converter = officeConverter ?: return emptyList()
        val chartRenderer = AndroidChartImageRenderer()
        return listOf(
            OdrDocxReader(converter, DocxChartRasterizer(chartRenderer)),
            OdrDocReader(converter),
            OdrOdtReader(converter),
            OdrRtfReader(converter),
            OdrPagesReader(converter),
            OdrXlsxReader(converter),
            OdrXlsReader(converter),
            OdrOdsReader(converter),
            OdrCsvReader(converter),
            OdrNumbersReader(converter),
            OdrPptxReader(converter, PptxChartRasterizer(chartRenderer)),
            OdrPptReader(converter),
            OdrOdpReader(converter),
            OdrKeynoteReader(converter),
            OdrOdgReader(converter)
        )
    }
}

data class DocumentCoreConfig(
    /** Folder inside `Context.cacheDir` for converted documents. */
    val cacheDirectoryName: String = "documents",
    /** Longer text files are cut, see `TextDocument.isTruncated`. */
    val maxTextChars: Int = 1_000_000
)
