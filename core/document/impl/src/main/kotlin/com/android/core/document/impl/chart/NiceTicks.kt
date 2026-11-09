package com.android.core.document.impl.chart

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal fun niceTicks(low: Double, high: Double): List<Double> {
    val range = if (high > low) high - low else 1.0
    val rough = range / 5
    val magnitude = 10.0.pow(floor(log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= rough }
    val start = floor(low / step) * step
    val end = ceil(high / step) * step
    val count = ((end - start) / step).toInt().coerceIn(1, 20)
    return (0..count).map { index -> start + index * step }
}
