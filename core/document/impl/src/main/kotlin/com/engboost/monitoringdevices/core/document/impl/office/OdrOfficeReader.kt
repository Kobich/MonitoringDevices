package com.engboost.monitoringdevices.core.document.impl.office

import android.util.Log
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader
import com.engboost.monitoringdevices.core.document.impl.chart.ChartRasterizer
import java.io.File

/**
 * Base of office readers backed by OpenDocument.core. A concrete reader only names
 * its API interface (e.g. `DocxReader`), which fixes the format.
 *
 * The library doesn't draw DrawingML charts: readers of formats that have them pass a
 * [chartRasterizer], which replaces charts with pictures before rendering. If that fails
 * (e.g. an encrypted document is not a zip), the unchanged file is rendered.
 */
internal abstract class OdrOfficeReader(
    private val converter: OdrHtmlConverter,
    private val chartRasterizer: ChartRasterizer? = null
) : DocumentReader<HtmlDocument> {

    override suspend fun read(source: DocumentSource, options: ReadOptions): HtmlDocument {
        val rasterizer = chartRasterizer ?: return converter.convert(
            source = source,
            format = formats.single(),
            password = options.password,
            variant = VARIANT_AS_IS
        )

        return converter.convert(
            source = source,
            format = formats.single(),
            password = options.password,
            variant = VARIANT_CHARTS_AS_PICTURES
        ) { sourceFile, workDirectory ->
            val output = File(workDirectory, "charts-as-pictures.${sourceFile.extension}")
            val replaced = runCatching { rasterizer.rasterize(sourceFile, output) }
                .onFailure { error -> Log.w(TAG, "Chart preprocessing failed for ${source.name}", error) }
                .getOrDefault(0)
            Log.d(TAG, "Replaced $replaced chart(s) in ${source.name}")
            if (replaced > 0) output else sourceFile
        }
    }

    private companion object {
        const val TAG = "OdrOfficeReader"
        const val VARIANT_AS_IS = "as-is"
        const val VARIANT_CHARTS_AS_PICTURES = "charts-as-pictures"
    }
}
