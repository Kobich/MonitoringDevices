package com.engboost.monitoringdevices.core.document.impl.office

import android.util.Log
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.impl.FormatReader
import com.engboost.monitoringdevices.core.document.impl.chart.ChartRasterizer
import java.io.File

/**
 * Office documents (docx, xlsx, pptx, odt, pages, ...) via OpenDocument.core.
 *
 * The library doesn't draw DrawingML charts: formats that have them pass a [chartRasterizer],
 * which replaces charts with pictures before rendering. If that fails (e.g. an encrypted
 * document is not a zip), the unchanged file is rendered.
 */
internal class OfficeReader(
    private val converter: OdrHtmlConverter,
    override val formats: Set<DocumentFormat>,
    private val chartRasterizer: ChartRasterizer? = null
) : FormatReader<HtmlDocument> {

    override suspend fun read(file: File, password: String?): HtmlDocument {
        val format = DocumentFormat.of(file) ?: formats.first()
        val rasterizer = chartRasterizer
            ?: return converter.convert(file, format, password, variant = VARIANT_AS_IS)

        return converter.convert(file, format, password, variant = VARIANT_CHARTS_AS_PICTURES) { original, workDirectory ->
            val output = File(workDirectory, "charts-as-pictures.${original.extension}")
            val replaced = runCatching { rasterizer.rasterize(original, output) }
                .onFailure { error -> Log.w(TAG, "Chart preprocessing failed for ${original.name}", error) }
                .getOrDefault(0)
            Log.d(TAG, "Replaced $replaced chart(s) in ${original.name}")
            if (replaced > 0) output else original
        }
    }

    private companion object {
        const val TAG = "OfficeReader"
        const val VARIANT_AS_IS = "as-is"
        const val VARIANT_CHARTS_AS_PICTURES = "charts-as-pictures"
    }
}
