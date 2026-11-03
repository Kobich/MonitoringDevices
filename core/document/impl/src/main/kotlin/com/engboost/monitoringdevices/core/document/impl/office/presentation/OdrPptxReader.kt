package com.engboost.monitoringdevices.core.document.impl.office.presentation

import com.engboost.monitoringdevices.core.document.api.format.presentation.PptxReader
import com.engboost.monitoringdevices.core.document.impl.chart.PptxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** PPTX via OpenDocument.core, charts drawn by [PptxChartRasterizer]. */
internal class OdrPptxReader(
    converter: OdrHtmlConverter,
    chartRasterizer: PptxChartRasterizer
) : OdrOfficeReader(converter, chartRasterizer), PptxReader
