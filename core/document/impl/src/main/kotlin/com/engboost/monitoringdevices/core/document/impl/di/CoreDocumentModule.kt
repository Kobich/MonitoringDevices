package com.engboost.monitoringdevices.core.document.impl.di

import com.engboost.monitoringdevices.core.document.api.cache.RenderedDocumentCache
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.core.document.api.comparison.DocumentReaderVariants
import com.engboost.monitoringdevices.core.document.api.reader.UniversalDocumentReader
import com.engboost.monitoringdevices.core.document.impl.DefaultDocumentReaderVariants
import com.engboost.monitoringdevices.core.document.impl.cache.DiskRenderedDocumentCache
import com.engboost.monitoringdevices.core.document.impl.chart.AndroidChartImageRenderer
import com.engboost.monitoringdevices.core.document.impl.chart.DocxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.source.DocumentSourceFiles
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

val coreDocumentModule = module {
    single { DiskRenderedDocumentCache(File(androidContext().cacheDir, "documents")) } bind RenderedDocumentCache::class
    single { DocumentSourceFiles(androidContext()) }
    single { OdrHtmlConverter(get(), get()) }
    single { DocxChartRasterizer(AndroidChartImageRenderer()) }

    single<DocumentReaderVariants> { DefaultDocumentReaderVariants(get(), get()) }
    // Default reader for code that doesn't take part in the comparison.
    single<UniversalDocumentReader> { get<DocumentReaderVariants>().reader(ChartRendering.AS_IS) }
}
