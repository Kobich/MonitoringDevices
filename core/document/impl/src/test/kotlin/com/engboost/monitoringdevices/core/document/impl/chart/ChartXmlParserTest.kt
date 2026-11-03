package com.engboost.monitoringdevices.core.document.impl.chart

import com.engboost.monitoringdevices.core.document.impl.ooxml.OoxmlXml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartXmlParserTest {
    private val parser = ChartXmlParser()

    @Test
    fun `parses clustered column chart with cached values`() {
        val chart = parser.parse(xml(ChartXmlFixtures.COLUMN_CHART))!!

        assertEquals("Sales 2026", chart.title)
        assertEquals(LegendPosition.BOTTOM, chart.legendPosition)

        val plot = chart.plots.single()
        assertEquals(PlotType.BAR, plot.type)
        assertEquals(BarDirection.COLUMN, plot.barDirection)
        assertEquals(Grouping.STANDARD, plot.grouping)
        assertEquals(219, plot.gapWidthPercent)
        assertEquals(listOf("Q1", "Q2", "Q3"), plot.categories)

        assertEquals(listOf("North", "South"), plot.series.map { it.name })
        assertEquals(listOf(4.3, 2.5, 3.5), plot.series[0].values)
        assertEquals("missing cached point stays empty", listOf(2.4, null, 1.8), plot.series[1].values)
        assertEquals(ChartTheme().accent(0), plot.series[0].color)
        assertEquals(0xFFFF0000.toInt(), plot.series[1].color)
    }

    @Test
    fun `scheme colors come from the document theme`() {
        val theme = parser.parseTheme(xml(ChartXmlFixtures.THEME))
        val chart = parser.parse(xml(ChartXmlFixtures.COLUMN_CHART), theme)!!

        assertEquals(0xFF112233.toInt(), theme.accent(0))
        assertEquals("missing accents keep Office defaults", ChartTheme.OFFICE_ACCENTS[2], theme.accent(2))
        assertEquals(0xFF112233.toInt(), chart.plots.single().series[0].color)
    }

    @Test
    fun `pie chart without title uses the series name and varies colors`() {
        val chart = parser.parse(xml(ChartXmlFixtures.PIE_CHART))!!
        val plot = chart.plots.single()

        assertEquals("Share", chart.title)
        assertNull(chart.legendPosition)
        assertEquals(PlotType.PIE, plot.type)
        assertEquals(30, plot.firstSliceAngle)
        assertEquals(true, plot.varyColors)
    }

    @Test
    fun `unsupported chart types are skipped`() {
        assertNull(parser.parse(xml(ChartXmlFixtures.UNSUPPORTED_CHART)))
    }

    private fun xml(text: String) = OoxmlXml.parse(text.byteInputStream())
}
