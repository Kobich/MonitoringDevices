package com.android.core.document.impl.chart

internal data class ChartModel(
    val title: String?,
    /** Several for a combo chart, e.g. bars + line. */
    val plots: List<ChartPlot>,
    val legendPosition: LegendPosition?
)

internal data class ChartPlot(
    val type: PlotType,
    val barDirection: BarDirection = BarDirection.COLUMN,
    val grouping: Grouping = Grouping.STANDARD,
    /** Percent of a bar width. */
    val gapWidthPercent: Int = 150,
    /** Percent of the radius. */
    val holeSizePercent: Int = 50,
    /** Degrees clockwise from 12 o'clock. */
    val firstSliceAngle: Int = 0,
    val varyColors: Boolean = false,
    val categories: List<String>,
    val series: List<ChartSeries>
)

internal data class ChartSeries(
    val name: String?,
    val values: List<Double?>,
    /** Scatter charts only. */
    val xValues: List<Double?>? = null,
    /** ARGB, `null` to use the theme palette by series index. */
    val color: Int? = null,
    /** `c:dPt`: point index -> ARGB. */
    val pointColors: Map<Int, Int> = emptyMap()
)

internal enum class PlotType { BAR, LINE, AREA, PIE, DOUGHNUT, SCATTER }

internal enum class BarDirection { COLUMN, BAR }

internal enum class Grouping { STANDARD, STACKED, PERCENT_STACKED }

internal enum class LegendPosition { RIGHT, LEFT, TOP, BOTTOM }

internal data class ChartTheme(
    val accents: List<Int> = OFFICE_ACCENTS
) {
    fun accent(index: Int): Int = accents[Math.floorMod(index, accents.size)]

    companion object {
        val OFFICE_ACCENTS = listOf(
            0xFF4472C4.toInt(),
            0xFFED7D31.toInt(),
            0xFFA5A5A5.toInt(),
            0xFFFFC000.toInt(),
            0xFF5B9BD5.toInt(),
            0xFF70AD47.toInt()
        )
    }
}
