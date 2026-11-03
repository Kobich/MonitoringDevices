package com.engboost.monitoringdevices.core.document.impl.chart

import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_CHART
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.NS_DRAWING
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.child
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.childElements
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.children
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.descendants
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.path
import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml.valAttribute
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Reads chart parts (`word/charts/chartN.xml`). Values come from the caches Word stores
 * next to the formulas (`c:numCache`, `c:strCache`); the embedded workbook is not opened.
 */
internal class ChartXmlParser {

    /** `null` if the chart has no plot type we can draw. */
    fun parse(chartPart: Document, theme: ChartTheme = ChartTheme()): ChartModel? {
        val chart = chartPart.documentElement.child(NS_CHART, "chart") ?: return null
        val plotArea = chart.child(NS_CHART, "plotArea") ?: return null

        val plots = plotArea.childElements().mapNotNull { element -> parsePlot(element, theme) }
        if (plots.isEmpty()) return null

        val isTitleDeleted = chart.child(NS_CHART, "autoTitleDeleted")?.valAttribute().isTrue()
        val titleElement = chart.child(NS_CHART, "title")
        val singleSeriesName = plots.singleOrNull()?.series?.singleOrNull()?.name
        val title = when {
            // An empty c:title is an automatic one: Word shows the only series name.
            titleElement != null -> richText(titleElement) ?: singleSeriesName
            // Without c:title Word still adds the automatic title unless it was deleted.
            !isTitleDeleted -> singleSeriesName
            else -> null
        }

        val legendPosition = chart.child(NS_CHART, "legend")?.let { legend ->
            when (legend.child(NS_CHART, "legendPos")?.valAttribute()) {
                "l" -> LegendPosition.LEFT
                "t" -> LegendPosition.TOP
                "b" -> LegendPosition.BOTTOM
                else -> LegendPosition.RIGHT
            }
        }

        return ChartModel(title = title, plots = plots, legendPosition = legendPosition)
    }

    /** Accent colors from a theme part (`word/theme/themeN.xml`). */
    fun parseTheme(themePart: Document): ChartTheme {
        val scheme = themePart.documentElement.descendants(NS_DRAWING, "clrScheme").firstOrNull()
            ?: return ChartTheme()
        val accents = (1..6).map { index ->
            scheme.child(NS_DRAWING, "accent$index")?.let(::colorOf) ?: ChartTheme.OFFICE_ACCENTS[index - 1]
        }
        return ChartTheme(accents)
    }

    private fun parsePlot(element: Element, theme: ChartTheme): ChartPlot? {
        if (element.namespaceURI != NS_CHART) return null
        val type = when (element.localName) {
            "barChart", "bar3DChart" -> PlotType.BAR
            "lineChart", "line3DChart" -> PlotType.LINE
            "areaChart", "area3DChart" -> PlotType.AREA
            "pieChart", "pie3DChart", "ofPieChart" -> PlotType.PIE
            "doughnutChart" -> PlotType.DOUGHNUT
            "scatterChart" -> PlotType.SCATTER
            else -> return null
        }

        val series = element.children(NS_CHART, "ser")
            .sortedBy { ser -> ser.child(NS_CHART, "order")?.valAttribute()?.toIntOrNull() ?: 0 }
        if (series.isEmpty()) return null

        val categories = series.firstNotNullOfOrNull { ser ->
            ser.child(NS_CHART, "cat")?.let(::categoryValues)?.takeIf { it.isNotEmpty() }
        }.orEmpty()

        return ChartPlot(
            type = type,
            barDirection = if (element.child(NS_CHART, "barDir")?.valAttribute() == "bar") {
                BarDirection.BAR
            } else {
                BarDirection.COLUMN
            },
            grouping = when (element.child(NS_CHART, "grouping")?.valAttribute()) {
                "stacked" -> Grouping.STACKED
                "percentStacked" -> Grouping.PERCENT_STACKED
                else -> Grouping.STANDARD
            },
            gapWidthPercent = element.child(NS_CHART, "gapWidth")?.valAttribute()?.toIntOrNull() ?: 150,
            holeSizePercent = element.child(NS_CHART, "holeSize")?.valAttribute()?.toIntOrNull() ?: 50,
            firstSliceAngle = element.child(NS_CHART, "firstSliceAng")?.valAttribute()?.toIntOrNull() ?: 0,
            varyColors = element.child(NS_CHART, "varyColors")?.valAttribute().isTrue() ||
                type == PlotType.PIE || type == PlotType.DOUGHNUT,
            categories = categories,
            series = series.map { ser -> parseSeries(ser, type, theme) }
        )
    }

    private fun parseSeries(ser: Element, type: PlotType, theme: ChartTheme): ChartSeries {
        val name = ser.child(NS_CHART, "tx")?.let { tx ->
            tx.path(NS_CHART, "strRef", "strCache")?.let(::points)?.firstOrNull()
                ?: tx.child(NS_CHART, "v")?.textContent
        }
        val values = (ser.child(NS_CHART, "val") ?: ser.child(NS_CHART, "yVal"))?.let(::numberValues).orEmpty()
        val xValues = ser.child(NS_CHART, "xVal")?.let(::numberValues)

        val shapeProperties = ser.child(NS_CHART, "spPr")
        // Lines are colored by their outline, everything else by its fill.
        val color = if (type == PlotType.LINE || type == PlotType.SCATTER) {
            shapeProperties?.path(NS_DRAWING, "ln", "solidFill")?.let { fill -> colorOf(fill, theme) }
                ?: shapeProperties?.child(NS_DRAWING, "solidFill")?.let { fill -> colorOf(fill, theme) }
        } else {
            shapeProperties?.child(NS_DRAWING, "solidFill")?.let { fill -> colorOf(fill, theme) }
        }

        val pointColors = ser.children(NS_CHART, "dPt").mapNotNull { point ->
            val index = point.child(NS_CHART, "idx")?.valAttribute()?.toIntOrNull() ?: return@mapNotNull null
            val fill = point.path(NS_CHART, "spPr")?.child(NS_DRAWING, "solidFill") ?: return@mapNotNull null
            colorOf(fill, theme)?.let { pointColor -> index to pointColor }
        }.toMap()

        return ChartSeries(
            name = name,
            values = values,
            xValues = xValues,
            color = color,
            pointColors = pointColors
        )
    }

    private fun categoryValues(cat: Element): List<String> {
        val cache = cat.path(NS_CHART, "strRef", "strCache")
            ?: cat.path(NS_CHART, "numRef", "numCache")
            ?: cat.path(NS_CHART, "multiLvlStrRef", "multiLvlStrCache")?.children(NS_CHART, "lvl")?.firstOrNull()
            ?: cat.child(NS_CHART, "strLit")
            ?: cat.child(NS_CHART, "numLit")
            ?: return emptyList()
        return points(cache).map { value -> value.orEmpty() }
    }

    private fun numberValues(element: Element): List<Double?> {
        val cache = element.path(NS_CHART, "numRef", "numCache")
            ?: element.child(NS_CHART, "numLit")
            ?: element.path(NS_CHART, "strRef", "strCache")
            ?: return emptyList()
        return points(cache).map { value -> value?.toDoubleOrNull() }
    }

    /** `c:pt idx=..` entries placed by index, missing points stay `null`. */
    private fun points(cache: Element): List<String?> {
        val declaredCount = cache.child(NS_CHART, "ptCount")?.valAttribute()?.toIntOrNull()
        val points = cache.children(NS_CHART, "pt").mapNotNull { pt ->
            val index = pt.getAttribute("idx").toIntOrNull() ?: return@mapNotNull null
            index to pt.child(NS_CHART, "v")?.textContent
        }
        val count = maxOf(declaredCount ?: 0, (points.maxOfOrNull { it.first } ?: -1) + 1)
        val values = arrayOfNulls<String>(count.coerceAtMost(MAX_POINTS))
        points.forEach { (index, value) -> if (index in values.indices) values[index] = value }
        return values.toList()
    }

    private fun richText(title: Element): String? {
        val text = title.descendants(NS_DRAWING, "p").joinToString("\n") { paragraph ->
            paragraph.descendants(NS_DRAWING, "t").joinToString("") { it.textContent }
        }.trim()
        return text.ifEmpty {
            title.path(NS_CHART, "tx", "strRef", "strCache")?.let(::points)?.firstOrNull()
        }
    }

    /** `a:srgbClr` or `a:schemeClr accentN` directly inside [element]. */
    private fun colorOf(element: Element, theme: ChartTheme = ChartTheme()): Int? {
        element.child(NS_DRAWING, "srgbClr")?.valAttribute()?.let { hex ->
            return hex.toLongOrNull(16)?.let { rgb -> (0xFF000000 or rgb).toInt() }
        }
        element.child(NS_DRAWING, "sysClr")?.getAttribute("lastClr")?.takeIf { it.isNotEmpty() }?.let { hex ->
            return hex.toLongOrNull(16)?.let { rgb -> (0xFF000000 or rgb).toInt() }
        }
        element.child(NS_DRAWING, "schemeClr")?.valAttribute()?.let { scheme ->
            val accentIndex = scheme.removePrefix("accent").toIntOrNull()
            if (scheme.startsWith("accent") && accentIndex != null) return theme.accent(accentIndex - 1)
        }
        return null
    }

    private fun String?.isTrue(): Boolean = this == "1" || this == "true"

    private companion object {
        const val MAX_POINTS = 10_000
    }
}
