package com.engboost.monitoringdevices.core.document.impl

import android.content.Context
import android.os.Build
import com.engboost.monitoringdevices.core.document.api.cache.RenderedDocumentCache
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader
import com.engboost.monitoringdevices.core.document.api.reader.UniversalDocumentReader
import com.engboost.monitoringdevices.core.document.impl.audio.AndroidAudioReader
import com.engboost.monitoringdevices.core.document.impl.cache.DiskRenderedDocumentCache
import com.engboost.monitoringdevices.core.document.impl.chart.AndroidChartImageRenderer
import com.engboost.monitoringdevices.core.document.impl.chart.DocxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.chart.PptxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.image.AndroidImageReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
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
 * val document = core.reader.read(DocumentSource.LocalFile(file))
 * ```
 */
class DocumentCore(
    context: Context,
    config: DocumentCoreConfig = DocumentCoreConfig()
) {
    private val appContext = context.applicationContext
    private val diskCache = DiskRenderedDocumentCache(File(appContext.cacheDir, config.cacheDirectoryName))
    private val sourceFiles = DocumentSourceFiles(appContext)

    /** On-disk output of converted documents (office files to HTML). */
    val cache: RenderedDocumentCache = diskCache

    /** Opens any supported file. */
    val reader: UniversalDocumentReader = UniversalDocumentReader(platformReaders(config) + officeReaders())

    /** PDF, text, images and audio on platform APIs, available on every API level. */
    private fun platformReaders(config: DocumentCoreConfig): List<DocumentReader<Document>> = listOf(
        AndroidPdfReader(sourceFiles),
        AndroidTextReader(sourceFiles, maxChars = config.maxTextChars),
        AndroidImageReader(sourceFiles),
        AndroidAudioReader(appContext)
    )

    /**
     * Office formats via OpenDocument.core. The library needs API 26: on older devices
     * its classes are never loaded and these formats are simply unsupported.
     */
    private fun officeReaders(): List<DocumentReader<Document>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()

        val converter = OdrHtmlConverter(diskCache, sourceFiles)
        val chartRenderer = AndroidChartImageRenderer()
        return listOf(
            OdrDocxReader(converter, DocxChartRasterizer(chartRenderer)),
            OdrDocReader(converter),
            OdrOdtReader(converter),
            OdrRtfReader(converter),
            OdrXlsxReader(converter),
            OdrXlsReader(converter),
            OdrOdsReader(converter),
            OdrCsvReader(converter),
            OdrPptxReader(converter, PptxChartRasterizer(chartRenderer)),
            OdrPptReader(converter),
            OdrOdpReader(converter)
        )
    }
}

data class DocumentCoreConfig(
    /** Folder inside `Context.cacheDir` for converted documents. */
    val cacheDirectoryName: String = "documents",
    /** Longer text files are cut, see `TextDocument.isTruncated`. */
    val maxTextChars: Int = 1_000_000
)
