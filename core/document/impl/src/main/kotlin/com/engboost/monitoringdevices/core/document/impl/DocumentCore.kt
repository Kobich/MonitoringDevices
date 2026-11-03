package com.engboost.monitoringdevices.core.document.impl

import android.content.Context
import android.os.Build
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.CSV
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.DOC
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.DOCX
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.KEYNOTE
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.NUMBERS
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.ODG
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.ODP
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.ODS
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.ODT
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.PAGES
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.PPT
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.PPTX
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.RTF
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.XLS
import com.engboost.monitoringdevices.core.document.api.DocumentFormat.XLSX
import com.engboost.monitoringdevices.core.document.api.DocumentReader
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.impl.audio.AudioReader
import com.engboost.monitoringdevices.core.document.impl.cache.RenderCache
import com.engboost.monitoringdevices.core.document.impl.chart.AndroidChartImageRenderer
import com.engboost.monitoringdevices.core.document.impl.chart.DocxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.chart.PptxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.image.ImageReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OfficeReader
import com.engboost.monitoringdevices.core.document.impl.pdf.PdfReader
import com.engboost.monitoringdevices.core.document.impl.svg.SvgReader
import com.engboost.monitoringdevices.core.document.impl.text.MarkdownReader
import com.engboost.monitoringdevices.core.document.impl.text.TextReader
import java.io.File

/**
 * Builds the [DocumentReader]. No DI framework inside: create one per process
 * (e.g. a Hilt `@Singleton`) and use `DocumentCore(context).reader.read(file)`.
 */
class DocumentCore(context: Context) {

    val reader: DocumentReader

    init {
        val cache = RenderCache(File(context.applicationContext.cacheDir, "documents"))
        // OpenDocument.core needs Android 8 (API 26); below it office formats aren't supported
        // and Markdown is shown as plain text.
        val converter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) OdrHtmlConverter(cache) else null
        val textReader = TextReader()

        val readers = mutableListOf<FormatReader<Document>>(
            PdfReader(),
            textReader,
            MarkdownReader(converter, textReader),
            ImageReader(),
            SvgReader(cache),
            AudioReader()
        )
        if (converter != null) {
            val charts = AndroidChartImageRenderer()
            readers += OfficeReader(converter, setOf(DOCX), DocxChartRasterizer(charts))
            readers += OfficeReader(converter, setOf(PPTX), PptxChartRasterizer(charts))
            readers += OfficeReader(
                converter,
                setOf(DOC, ODT, RTF, PAGES, XLSX, XLS, ODS, CSV, NUMBERS, PPT, ODP, KEYNOTE, ODG)
            )
        }
        reader = CompositeDocumentReader(readers)
    }
}
