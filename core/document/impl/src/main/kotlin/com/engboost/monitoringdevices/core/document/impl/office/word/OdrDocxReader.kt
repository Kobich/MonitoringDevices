package com.engboost.monitoringdevices.core.document.impl.office.word

import com.engboost.monitoringdevices.core.document.api.reader.word.DocxReader
import com.engboost.monitoringdevices.core.document.impl.chart.DocxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** DOCX via OpenDocument.core, charts drawn by [DocxChartRasterizer]. */
internal class OdrDocxReader(
    converter: OdrHtmlConverter,
    chartRasterizer: DocxChartRasterizer
) : OdrOfficeReader(converter, chartRasterizer), DocxReader
