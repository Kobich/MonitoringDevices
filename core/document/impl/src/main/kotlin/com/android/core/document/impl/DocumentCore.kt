package com.android.core.document.impl

import android.content.Context
import android.os.Build
import com.android.core.document.api.DocumentFormat.CSV
import com.android.core.document.api.DocumentFormat.DOC
import com.android.core.document.api.DocumentFormat.DOCX
import com.android.core.document.api.DocumentFormat.KEYNOTE
import com.android.core.document.api.DocumentFormat.NUMBERS
import com.android.core.document.api.DocumentFormat.ODG
import com.android.core.document.api.DocumentFormat.ODP
import com.android.core.document.api.DocumentFormat.ODS
import com.android.core.document.api.DocumentFormat.ODT
import com.android.core.document.api.DocumentFormat.PAGES
import com.android.core.document.api.DocumentFormat.PPT
import com.android.core.document.api.DocumentFormat.PPTX
import com.android.core.document.api.DocumentFormat.RTF
import com.android.core.document.api.DocumentFormat.XLS
import com.android.core.document.api.DocumentFormat.XLSX
import com.android.core.document.api.DocumentReader
import com.android.core.document.api.document.Document
import com.android.core.document.impl.audio.AudioReader
import com.android.core.document.impl.cache.RenderCache
import com.android.core.document.impl.chart.ChartImageRenderer
import com.android.core.document.impl.chart.DocxChartRasterizer
import com.android.core.document.impl.chart.PptxChartRasterizer
import com.android.core.document.impl.image.ImageReader
import com.android.core.document.impl.office.OdrHtmlConverter
import com.android.core.document.impl.office.OfficeReader
import com.android.core.document.impl.pdf.PdfReader
import com.android.core.document.impl.svg.SvgReader
import com.android.core.document.impl.text.MarkdownReader
import com.android.core.document.impl.text.TextReader
import com.android.core.document.impl.video.VideoReader
import java.io.File

/** Create one per process: readers share the conversion cache and its lock. */
class DocumentCore(context: Context) {

    val reader: DocumentReader

    init {
        val cache = RenderCache(File(context.applicationContext.cacheDir, "documents"))
        // OpenDocument.core needs Android 8 (API 26); below it office formats aren't supported
        // and Markdown is shown as plain text.
        val converter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) OdrHtmlConverter(cache) else null
        val textReader = TextReader()
        val audioReader = AudioReader()

        val readers = mutableListOf<FormatReader<Document>>(
            PdfReader(),
            textReader,
            MarkdownReader(converter, textReader),
            ImageReader(),
            SvgReader(cache),
            audioReader,
            VideoReader(audioReader)
        )
        if (converter != null) {
            val charts = ChartImageRenderer()
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
