package com.engboost.monitoringdevices.core.document.impl

import android.content.Context
import com.engboost.monitoringdevices.core.document.api.cache.RenderedDocumentCache
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.core.document.api.comparison.DocumentReaderVariants
import com.engboost.monitoringdevices.core.document.api.reader.UniversalDocumentReader
import com.engboost.monitoringdevices.core.document.impl.audio.AndroidAudioReader
import com.engboost.monitoringdevices.core.document.impl.cache.DiskRenderedDocumentCache
import com.engboost.monitoringdevices.core.document.impl.chart.AndroidChartImageRenderer
import com.engboost.monitoringdevices.core.document.impl.chart.DocxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.image.AndroidImageReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
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

    /** TEMPORARY: chart comparison, see [ChartRendering]. */
    val readerVariants: DocumentReaderVariants = DefaultDocumentReaderVariants(
        converter = OdrHtmlConverter(diskCache, sourceFiles),
        rasterizer = DocxChartRasterizer(AndroidChartImageRenderer()),
        platformReaders = listOf(
            AndroidPdfReader(sourceFiles),
            AndroidTextReader(sourceFiles, maxChars = config.maxTextChars),
            AndroidImageReader(sourceFiles),
            AndroidAudioReader(appContext)
        )
    )

    /** Opens any supported file. */
    val reader: UniversalDocumentReader = readerVariants.reader(ChartRendering.AS_IS)
}

data class DocumentCoreConfig(
    /** Folder inside `Context.cacheDir` for converted documents. */
    val cacheDirectoryName: String = "documents",
    /** Longer text files are cut, see `TextDocument.isTruncated`. */
    val maxTextChars: Int = 1_000_000
)
