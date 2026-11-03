package com.engboost.monitoringdevices.core.document.impl.chart

import java.io.File

/** Writes a copy of an office document with its charts replaced by pictures. */
internal fun interface ChartRasterizer {
    /** @return number of replaced charts; [output] is written only when it is above zero. */
    fun rasterize(input: File, output: File): Int
}
