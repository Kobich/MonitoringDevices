package com.engboost.monitoringdevices.core.document.impl.chart

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Draws a chart into PNG bytes with `android.graphics`, following the look of Word's default chart style. */
internal class ChartImageRenderer {
    fun render(chart: ChartModel, theme: ChartTheme, widthPx: Int, heightPx: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        try {
            ChartPainter(Canvas(bitmap), widthPx.toFloat(), heightPx.toFloat(), chart, theme).paint()
            return ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}

private class ChartPainter(
    private val canvas: Canvas,
    private val width: Float,
    private val height: Float,
    private val chart: ChartModel,
    private val theme: ChartTheme
) {
    private fun pt(points: Double): Float = (points * ChartScale.PX_PER_POINT).toFloat()

    private val textColor = 0xFF595959.toInt()
    private val gridColor = 0xFFD9D9D9.toInt()
    private val axisColor = 0xFFBFBFBF.toInt()

    private val titlePaint = textPaint(14.0)
    private val labelPaint = textPaint(9.0)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val numberFormat = DecimalFormat("#,##0.##")

    fun paint() {
        canvas.drawColor(Color.WHITE)
        val area = RectF(pt(7.0), pt(7.0), width - pt(7.0), height - pt(7.0))

        chart.title?.let { title -> drawTitle(title, area) }
        chart.legendPosition?.let { position -> drawLegend(position, area) }

        val plots = chart.plots
        when {
            plots.any { plot -> plot.type == PlotType.PIE || plot.type == PlotType.DOUGHNUT } ->
                drawPie(plots.first { plot -> plot.type == PlotType.PIE || plot.type == PlotType.DOUGHNUT }, area)

            plots.any { plot -> plot.type == PlotType.SCATTER } ->
                drawScatter(plots.filter { plot -> plot.type == PlotType.SCATTER }, area)

            else -> drawCategoryPlots(plots, area)
        }
    }

    // region Title and legend

    private fun drawTitle(title: String, area: RectF) {
        val lines = title.lines().filter { line -> line.isNotBlank() }
        var baseline = area.top - titlePaint.ascent()
        lines.forEach { line ->
            canvas.drawText(ellipsize(line, titlePaint, area.width()), area.centerX(), baseline, titlePaint)
            baseline += titlePaint.fontSpacing
        }
        area.top += lines.size * titlePaint.fontSpacing + pt(6.0)
    }

    private class LegendItem(val label: String, val color: Int)

    private fun legendItems(): List<LegendItem> {
        val pie = chart.plots.firstOrNull { plot -> plot.type == PlotType.PIE || plot.type == PlotType.DOUGHNUT }
        if (pie != null) {
            val series = pie.series.firstOrNull() ?: return emptyList()
            return pie.categories.mapIndexed { index, category ->
                LegendItem(category, series.pointColors[index] ?: theme.accent(index))
            }
        }
        var seriesIndex = 0
        return chart.plots.flatMap { plot ->
            plot.series.map { series ->
                val index = seriesIndex++
                LegendItem(series.name ?: "Series ${index + 1}", seriesColor(series, index))
            }
        }
    }

    private fun drawLegend(position: LegendPosition, area: RectF) {
        val items = legendItems()
        if (items.isEmpty()) return

        val marker = pt(5.0)
        val gap = pt(4.0)
        val itemSpacing = pt(10.0)
        val lineHeight = labelPaint.fontSpacing
        fun itemWidth(item: LegendItem) = marker + gap + labelPaint.measureText(item.label)

        when (position) {
            LegendPosition.TOP, LegendPosition.BOTTOM -> {
                // Wrap items into centered rows.
                val rows = mutableListOf<MutableList<LegendItem>>(mutableListOf())
                var rowWidth = 0f
                items.forEach { item ->
                    val itemWidth = itemWidth(item)
                    if (rows.last().isNotEmpty() && rowWidth + itemSpacing + itemWidth > area.width()) {
                        rows += mutableListOf<LegendItem>()
                        rowWidth = 0f
                    }
                    rowWidth += (if (rows.last().isEmpty()) 0f else itemSpacing) + itemWidth
                    rows.last() += item
                }
                val blockHeight = rows.size * lineHeight
                var top = if (position == LegendPosition.TOP) area.top else area.bottom - blockHeight
                rows.forEach { row ->
                    val totalWidth = row.sumOf { itemWidth(it).toDouble() }.toFloat() + itemSpacing * (row.size - 1)
                    var left = area.centerX() - totalWidth / 2
                    row.forEach { item ->
                        drawLegendItem(item, left, top, marker, gap, lineHeight)
                        left += itemWidth(item) + itemSpacing
                    }
                    top += lineHeight
                }
                if (position == LegendPosition.TOP) area.top += blockHeight + pt(6.0) else area.bottom -= blockHeight + pt(6.0)
            }

            LegendPosition.LEFT, LegendPosition.RIGHT -> {
                val columnWidth = min(items.maxOf(::itemWidth), area.width() * 0.4f)
                val blockHeight = items.size * lineHeight
                val left = if (position == LegendPosition.LEFT) area.left else area.right - columnWidth
                var top = area.centerY() - blockHeight / 2
                items.forEach { item ->
                    drawLegendItem(item, left, top, marker, gap, lineHeight, columnWidth)
                    top += lineHeight
                }
                if (position == LegendPosition.LEFT) area.left += columnWidth + pt(8.0) else area.right -= columnWidth + pt(8.0)
            }
        }
    }

    private fun drawLegendItem(
        item: LegendItem,
        left: Float,
        top: Float,
        marker: Float,
        gap: Float,
        lineHeight: Float,
        maxWidth: Float = Float.MAX_VALUE
    ) {
        val centerY = top + lineHeight / 2
        fillPaint.color = item.color
        canvas.drawRect(left, centerY - marker / 2, left + marker, centerY + marker / 2, fillPaint)
        val paint = Paint(labelPaint).apply { textAlign = Paint.Align.LEFT }
        val text = ellipsize(item.label, paint, maxWidth - marker - gap)
        canvas.drawText(text, left + marker + gap, centerY - (paint.ascent() + paint.descent()) / 2, paint)
    }

    // endregion

    // region Category charts: bars, lines, areas

    private fun drawCategoryPlots(plots: List<ChartPlot>, area: RectF) {
        val horizontal = plots.any { plot -> plot.type == PlotType.BAR && plot.barDirection == BarDirection.BAR }
        // Word can't combine horizontal bars with lines; keep the bars only in that case.
        val drawable = if (horizontal) plots.filter { plot -> plot.type == PlotType.BAR } else plots
        val categoryCount = drawable.maxOf { plot ->
            max(plot.categories.size, plot.series.maxOfOrNull { series -> series.values.size } ?: 0)
        }
        if (categoryCount == 0) return
        val categories = drawable.firstOrNull { plot -> plot.categories.isNotEmpty() }?.categories.orEmpty()

        var minValue = 0.0
        var maxValue = 0.0
        drawable.forEach { plot ->
            val (plotMin, plotMax) = valueRange(plot, categoryCount)
            minValue = min(minValue, plotMin)
            maxValue = max(maxValue, plotMax)
        }
        if (maxValue == minValue) maxValue = minValue + 1
        val ticks = niceTicks(minValue, maxValue)
        val isPercent = drawable.all { plot -> plot.grouping == Grouping.PERCENT_STACKED }
        val tickLabels = ticks.map { tick -> formatNumber(tick) + if (isPercent) "%" else "" }

        // Reserve space for the axis labels.
        val valueLabelWidth = tickLabels.maxOf { label -> labelPaint.measureText(label) } + pt(4.0)
        val labelHeight = labelPaint.fontSpacing + pt(2.0)
        val categoryLabelWidth = if (horizontal) {
            min(categories.maxOfOrNull { labelPaint.measureText(it) } ?: 0f, area.width() * 0.3f) + pt(4.0)
        } else {
            0f
        }
        val plotRect = if (horizontal) {
            RectF(area.left + categoryLabelWidth, area.top, area.right - pt(4.0), area.bottom - labelHeight)
        } else {
            RectF(area.left + valueLabelWidth, area.top + labelPaint.fontSpacing / 2, area.right, area.bottom - labelHeight)
        }
        if (plotRect.width() <= 0 || plotRect.height() <= 0) return

        val axis = CategoryAxis(plotRect, horizontal, ticks.first(), ticks.last(), categoryCount)
        drawValueGrid(axis, ticks, tickLabels)
        drawCategoryLabels(axis, categories)

        var seriesOffset = 0
        drawable.forEach { plot ->
            when (plot.type) {
                PlotType.BAR -> drawBars(plot, axis, seriesOffset)
                PlotType.LINE -> drawLines(plot, axis, seriesOffset)
                PlotType.AREA -> drawAreas(plot, axis, seriesOffset)
                else -> Unit
            }
            seriesOffset += plot.series.size
        }

        strokePaint.color = axisColor
        strokePaint.strokeWidth = pt(0.75)
        if (horizontal) {
            val x = axis.valuePosition(0.0)
            canvas.drawLine(x, plotRect.top, x, plotRect.bottom, strokePaint)
        } else {
            val y = axis.valuePosition(0.0)
            canvas.drawLine(plotRect.left, y, plotRect.right, y, strokePaint)
        }
    }

    private class CategoryAxis(
        val plot: RectF,
        val horizontal: Boolean,
        val min: Double,
        val max: Double,
        val categoryCount: Int
    ) {
        val slot: Float = (if (horizontal) plot.height() else plot.width()) / categoryCount

        fun valuePosition(value: Double): Float {
            val fraction = ((value - min) / (max - min)).toFloat()
            return if (horizontal) plot.left + fraction * plot.width() else plot.bottom - fraction * plot.height()
        }

        /** Start of the category slot along the category axis; Word puts the first bar category at the bottom. */
        fun slotStart(index: Int): Float {
            return if (horizontal) plot.bottom - (index + 1) * slot else plot.left + index * slot
        }

        fun slotCenter(index: Int): Float = slotStart(index) + slot / 2

        /** Rectangle between two values across a part of a slot. */
        fun bar(slotOffsetStart: Float, slotOffsetEnd: Float, index: Int, from: Double, to: Double): RectF {
            val start = slotStart(index)
            val a = valuePosition(from)
            val b = valuePosition(to)
            return if (horizontal) {
                RectF(min(a, b), start + slotOffsetStart, max(a, b), start + slotOffsetEnd)
            } else {
                RectF(start + slotOffsetStart, min(a, b), start + slotOffsetEnd, max(a, b))
            }
        }
    }

    private fun valueRange(plot: ChartPlot, categoryCount: Int): Pair<Double, Double> {
        return when (plot.grouping) {
            Grouping.PERCENT_STACKED -> 0.0 to 100.0
            Grouping.STACKED -> {
                var low = 0.0
                var high = 0.0
                repeat(categoryCount) { index ->
                    val values = plot.series.map { series -> series.values.getOrNull(index) ?: 0.0 }
                    high = max(high, values.filter { it > 0 }.sum())
                    low = min(low, values.filter { it < 0 }.sum())
                }
                low to high
            }

            Grouping.STANDARD -> {
                val values = plot.series.flatMap { series -> series.values.filterNotNull() }
                (values.minOrNull() ?: 0.0) to (values.maxOrNull() ?: 0.0)
            }
        }
    }

    /** Values of a category for drawing: percent of the category total for 100% charts. */
    private fun plotValue(plot: ChartPlot, seriesIndex: Int, categoryIndex: Int): Double? {
        val value = plot.series[seriesIndex].values.getOrNull(categoryIndex) ?: return null
        if (plot.grouping != Grouping.PERCENT_STACKED) return value
        val total = plot.series.sumOf { series -> abs(series.values.getOrNull(categoryIndex) ?: 0.0) }
        return if (total == 0.0) 0.0 else value / total * 100
    }

    private fun drawValueGrid(axis: CategoryAxis, ticks: List<Double>, labels: List<String>) {
        strokePaint.color = gridColor
        strokePaint.strokeWidth = pt(0.75)
        val paint = Paint(labelPaint)
        ticks.forEachIndexed { index, tick ->
            val position = axis.valuePosition(tick)
            if (axis.horizontal) {
                canvas.drawLine(position, axis.plot.top, position, axis.plot.bottom, strokePaint)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(labels[index], position, axis.plot.bottom + pt(2.0) - paint.ascent(), paint)
            } else {
                canvas.drawLine(axis.plot.left, position, axis.plot.right, position, strokePaint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(
                    labels[index],
                    axis.plot.left - pt(4.0),
                    position - (paint.ascent() + paint.descent()) / 2,
                    paint
                )
            }
        }
    }

    private fun drawCategoryLabels(axis: CategoryAxis, categories: List<String>) {
        if (categories.isEmpty()) return
        val paint = Paint(labelPaint)
        if (axis.horizontal) {
            paint.textAlign = Paint.Align.RIGHT
            val step = labelStep(paint.fontSpacing, axis.slot)
            categories.forEachIndexed { index, category ->
                if (index % step != 0) return@forEachIndexed
                val text = ellipsize(category, paint, axis.plot.left - pt(4.0))
                canvas.drawText(text, axis.plot.left - pt(4.0), axis.slotCenter(index) - (paint.ascent() + paint.descent()) / 2, paint)
            }
        } else {
            paint.textAlign = Paint.Align.CENTER
            val widest = categories.maxOf { paint.measureText(it) } + pt(4.0)
            val step = labelStep(widest, axis.slot)
            categories.forEachIndexed { index, category ->
                if (index % step != 0) return@forEachIndexed
                val text = ellipsize(category, paint, axis.slot * step)
                canvas.drawText(text, axis.slotCenter(index), axis.plot.bottom + pt(2.0) - paint.ascent(), paint)
            }
        }
    }

    private fun labelStep(labelSize: Float, slot: Float): Int = max(1, ceil(labelSize / slot).toInt())

    private fun drawBars(plot: ChartPlot, axis: CategoryAxis, seriesOffset: Int) {
        val gap = plot.gapWidthPercent / 100f
        val seriesCount = plot.series.size
        val isStacked = plot.grouping != Grouping.STANDARD
        val barsPerSlot = if (isStacked) 1 else seriesCount
        val barWidth = axis.slot / (barsPerSlot + gap)
        val clusterStart = barWidth * gap / 2

        repeat(axis.categoryCount) { category ->
            var positiveTop = 0.0
            var negativeBottom = 0.0
            plot.series.forEachIndexed { seriesIndex, series ->
                val value = plotValue(plot, seriesIndex, category) ?: return@forEachIndexed
                val rect = if (isStacked) {
                    val from = if (value >= 0) positiveTop else negativeBottom
                    val to = from + value
                    if (value >= 0) positiveTop = to else negativeBottom = to
                    axis.bar(clusterStart, clusterStart + barWidth, category, from, to)
                } else {
                    // Word draws the first series closest to the axis origin: left, or bottom for bars.
                    val position = if (axis.horizontal) seriesCount - 1 - seriesIndex else seriesIndex
                    val start = clusterStart + position * barWidth
                    axis.bar(start, start + barWidth, category, 0.0, value)
                }
                fillPaint.color = pointColor(plot, series, seriesOffset + seriesIndex, category)
                canvas.drawRect(rect, fillPaint)
            }
        }
    }

    private fun drawLines(plot: ChartPlot, axis: CategoryAxis, seriesOffset: Int) {
        val stackedTotals = DoubleArray(axis.categoryCount)
        plot.series.forEachIndexed { seriesIndex, series ->
            val path = Path()
            var isDrawing = false
            repeat(axis.categoryCount) { category ->
                val raw = plotValue(plot, seriesIndex, category)
                if (raw == null) {
                    isDrawing = false
                    return@repeat
                }
                val value = if (plot.grouping == Grouping.STANDARD) raw else (stackedTotals[category] + raw).also {
                    stackedTotals[category] = it
                }
                val x = axis.slotCenter(category)
                val y = axis.valuePosition(value)
                if (isDrawing) path.lineTo(x, y) else path.moveTo(x, y)
                isDrawing = true
            }
            strokePaint.color = seriesColor(series, seriesOffset + seriesIndex)
            strokePaint.strokeWidth = pt(2.25)
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun drawAreas(plot: ChartPlot, axis: CategoryAxis, seriesOffset: Int) {
        var previous = DoubleArray(axis.categoryCount)
        val isStacked = plot.grouping != Grouping.STANDARD
        plot.series.forEachIndexed { seriesIndex, series ->
            val top = DoubleArray(axis.categoryCount) { category ->
                val value = plotValue(plot, seriesIndex, category) ?: 0.0
                if (isStacked) previous[category] + value else value
            }
            val bottom = if (isStacked) previous else DoubleArray(axis.categoryCount)
            val path = Path()
            repeat(axis.categoryCount) { category ->
                val x = axis.slotCenter(category)
                val y = axis.valuePosition(top[category])
                if (category == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            for (category in axis.categoryCount - 1 downTo 0) {
                path.lineTo(axis.slotCenter(category), axis.valuePosition(bottom[category]))
            }
            path.close()
            fillPaint.color = seriesColor(series, seriesOffset + seriesIndex)
            canvas.drawPath(path, fillPaint)
            if (isStacked) previous = top
        }
    }

    // endregion

    // region Pie and scatter

    private fun drawPie(plot: ChartPlot, area: RectF) {
        val series = plot.series.firstOrNull() ?: return
        val values = series.values.map { value -> max(0.0, value ?: 0.0) }
        val total = values.sum()
        if (total <= 0) return

        val radius = min(area.width(), area.height()) / 2 * 0.95f
        val oval = RectF(area.centerX() - radius, area.centerY() - radius, area.centerX() + radius, area.centerY() + radius)
        var angle = -90f + plot.firstSliceAngle
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = pt(1.0)

        values.forEachIndexed { index, value ->
            if (value <= 0) return@forEachIndexed
            val sweep = (value / total * 360).toFloat()
            fillPaint.color = series.pointColors[index] ?: theme.accent(index)
            canvas.drawArc(oval, angle, sweep, true, fillPaint)
            canvas.drawArc(oval, angle, sweep, true, strokePaint)
            angle += sweep
        }
        if (plot.type == PlotType.DOUGHNUT) {
            fillPaint.color = Color.WHITE
            canvas.drawCircle(oval.centerX(), oval.centerY(), radius * plot.holeSizePercent / 100f, fillPaint)
        }
    }

    private fun drawScatter(plots: List<ChartPlot>, area: RectF) {
        val series = plots.flatMap { plot -> plot.series }
        val points = series.map { item ->
            item.values.mapIndexedNotNull { index, y ->
                val x = item.xValues?.getOrNull(index) ?: (index + 1).toDouble()
                if (y == null) null else x to y
            }
        }
        val all = points.flatten()
        if (all.isEmpty()) return

        val xTicks = niceTicks(min(0.0, all.minOf { it.first }), all.maxOf { it.first })
        val yTicks = niceTicks(min(0.0, all.minOf { it.second }), all.maxOf { it.second })
        val yLabels = yTicks.map(::formatNumber)
        val xLabels = xTicks.map(::formatNumber)

        val labelWidth = yLabels.maxOf { labelPaint.measureText(it) } + pt(4.0)
        val plot = RectF(area.left + labelWidth, area.top + labelPaint.fontSpacing / 2, area.right - pt(8.0), area.bottom - labelPaint.fontSpacing - pt(2.0))
        if (plot.width() <= 0 || plot.height() <= 0) return

        fun xPosition(x: Double) = plot.left + ((x - xTicks.first()) / (xTicks.last() - xTicks.first())).toFloat() * plot.width()
        fun yPosition(y: Double) = plot.bottom - ((y - yTicks.first()) / (yTicks.last() - yTicks.first())).toFloat() * plot.height()

        strokePaint.color = gridColor
        strokePaint.strokeWidth = pt(0.75)
        val paint = Paint(labelPaint)
        yTicks.forEachIndexed { index, tick ->
            val y = yPosition(tick)
            canvas.drawLine(plot.left, y, plot.right, y, strokePaint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(yLabels[index], plot.left - pt(4.0), y - (paint.ascent() + paint.descent()) / 2, paint)
        }
        xTicks.forEachIndexed { index, tick ->
            val x = xPosition(tick)
            canvas.drawLine(x, plot.top, x, plot.bottom, strokePaint)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(xLabels[index], x, plot.bottom + pt(2.0) - paint.ascent(), paint)
        }

        points.forEachIndexed { index, seriesPoints ->
            fillPaint.color = seriesColor(series[index], index)
            seriesPoints.forEach { (x, y) -> canvas.drawCircle(xPosition(x), yPosition(y), pt(2.5), fillPaint) }
        }
    }

    // endregion

    private fun seriesColor(series: ChartSeries, index: Int): Int = series.color ?: theme.accent(index)

    private fun pointColor(plot: ChartPlot, series: ChartSeries, seriesIndex: Int, point: Int): Int {
        series.pointColors[point]?.let { return it }
        return if (plot.varyColors && plot.series.size == 1) theme.accent(point) else seriesColor(series, seriesIndex)
    }

    private fun formatNumber(value: Double): String = numberFormat.format(if (abs(value) < 1e-9) 0.0 else value)

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + paint.measureText("…") > maxWidth) end--
        return if (end == 0) "" else text.substring(0, end) + "…"
    }

    private fun textPaint(points: Double) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = pt(points)
        textAlign = Paint.Align.CENTER
    }
}
