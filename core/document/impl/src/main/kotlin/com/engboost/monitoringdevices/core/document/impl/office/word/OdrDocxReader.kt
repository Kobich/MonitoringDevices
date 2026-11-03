package com.engboost.monitoringdevices.core.document.impl.office.word

import android.util.Log
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.reader.word.DocxReader
import com.engboost.monitoringdevices.core.document.impl.chart.DocxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import java.io.File

/**
 * DOCX via OpenDocument.core. The library doesn't draw DrawingML charts, so before rendering
 * every chart is replaced with a picture drawn by [DocxChartRasterizer]. If preprocessing fails
 * (e.g. an encrypted document is not a zip) the unchanged file is rendered.
 */
internal class OdrDocxReader(
    private val converter: OdrHtmlConverter,
    private val rasterizer: DocxChartRasterizer
) : DocxReader {

    override suspend fun read(source: DocumentSource, options: ReadOptions): HtmlDocument {
        return converter.convert(
            source = source,
            format = DocumentFormat.DOCX,
            password = options.password,
            variant = VARIANT
        ) { sourceFile, workDirectory ->
            val output = File(workDirectory, "charts-as-pictures.docx")
            val replaced = runCatching { rasterizer.rasterize(sourceFile, output) }
                .onFailure { error -> Log.w(TAG, "Chart preprocessing failed for ${source.name}", error) }
                .getOrDefault(0)
            Log.d(TAG, "Replaced $replaced chart(s) in ${source.name}")
            if (replaced > 0) output else sourceFile
        }
    }

    private companion object {
        const val TAG = "OdrDocxReader"
        const val VARIANT = "charts-as-pictures"
    }
}
