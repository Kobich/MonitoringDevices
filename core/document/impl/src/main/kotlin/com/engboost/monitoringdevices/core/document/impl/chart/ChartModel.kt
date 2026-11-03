package com.engboost.monitoringdevices.core.document.impl.chart

/** What we understand of a DrawingML chart (`c:chartSpace`), enough to draw it. */
internal data class ChartModel(
    val title: String?,
    /** One plot for a simple chart, several for a combo chart (e.g. bars + line). */
    val plots: List<ChartPlot>,
    /** `null` when the chart has no legend. */
    val legendPosition: LegendPosition?
)

internal data class ChartPlot(
    val type: PlotType,
    /** For [PlotType.BAR]: vertical columns or horizontal bars. */
    val barDirection: BarDirection = BarDirection.COLUMN,
    val grouping: Grouping = Grouping.STANDARD,
    /** Gap between bar clusters in percent of a bar width (`c:gapWidth`). */
    val gapWidthPercent: Int = 150,
    /** For [PlotType.DOUGHNUT]: hole size in percent of the radius. */
    val holeSizePercent: Int = 50,
    /** For pie charts: angle of the first slice in degrees, clockwise from 12 o'clock. */
    val firstSliceAngle: Int = 0,
    /** Pie and other charts with `c:varyColors`: every point gets its own color. */
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
    /** Per-point overrides (`c:dPt`), mostly for pie slices. */
    val pointColors: Map<Int, Int> = emptyMap()
)

internal enum class PlotType { BAR, LINE, AREA, PIE, DOUGHNUT, SCATTER }

internal enum class BarDirection { COLUMN, BAR }

internal enum class Grouping { STANDARD, STACKED, PERCENT_STACKED }

internal enum class LegendPosition { RIGHT, LEFT, TOP, BOTTOM }

/** Theme accent colors (`a:accent1`..`a:accent6`), ARGB. Defaults are the Office theme. */
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
