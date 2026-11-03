package com.engboost.monitoringdevices.core.document.impl.chart

/** Draws a chart into an image file content (PNG). */
internal fun interface ChartImageRenderer {
    fun render(chart: ChartModel, theme: ChartTheme, widthPx: Int, heightPx: Int): ByteArray
}

/** Pixel density of chart pictures, shared by the rasterizer and the renderer. */
internal object ChartScale {
    /** Pictures are drawn at 2x of 96 dpi to stay sharp when zoomed in. */
    const val RENDER_SCALE = 2.0
    const val PX_PER_POINT = 96.0 / 72.0 * RENDER_SCALE
}
