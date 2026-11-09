package com.android.core.document.impl.chart

internal object ChartScale {
    /** Pictures are drawn at 2x of 96 dpi to stay sharp when zoomed in. */
    const val RENDER_SCALE = 2.0
    const val PX_PER_POINT = 96.0 / 72.0 * RENDER_SCALE

    /** 914400 EMU per inch / 96 px per inch. */
    private const val EMU_PER_PX = 9525.0
    private const val MIN_SIDE_PX = 64
    private const val MAX_SIDE_PX = 2400

    fun emuToPx(emu: Long): Int = (emu / EMU_PER_PX * RENDER_SCALE).toInt().coerceIn(MIN_SIDE_PX, MAX_SIDE_PX)
}
