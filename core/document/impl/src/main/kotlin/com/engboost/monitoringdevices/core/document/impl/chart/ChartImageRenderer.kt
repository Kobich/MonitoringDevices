package com.engboost.monitoringdevices.core.document.impl.chart

/**
 * Draws a chart into an image file content (PNG). Production uses [AndroidChartImageRenderer];
 * JVM tests of the rasterizers pass a fake, since `android.graphics` isn't available there.
 */
internal fun interface ChartImageRenderer {
    fun render(chart: ChartModel, theme: ChartTheme, widthPx: Int, heightPx: Int): ByteArray
}
