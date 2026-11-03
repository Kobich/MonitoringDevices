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

    /** 914400 EMU per inch / 96 px per inch. */
    private const val EMU_PER_PX = 9525.0
    private const val MIN_SIDE_PX = 64
    private const val MAX_SIDE_PX = 2400

    /** Picture size for a drawing of [emu] English Metric Units. */
    fun emuToPx(emu: Long): Int = (emu / EMU_PER_PX * RENDER_SCALE).toInt().coerceIn(MIN_SIDE_PX, MAX_SIDE_PX)
}

/** Writes a copy of an office document with its charts replaced by pictures. */
internal fun interface ChartRasterizer {
    /** @return number of replaced charts; [output] is written only when it is above zero. */
    fun rasterize(input: java.io.File, output: java.io.File): Int
}
