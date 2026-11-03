package com.engboost.monitoringdevices.core.document.impl

import android.os.Build
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.core.document.api.comparison.DocumentReaderVariants
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader
import com.engboost.monitoringdevices.core.document.api.reader.UniversalDocumentReader
import com.engboost.monitoringdevices.core.document.impl.chart.DocxChartRasterizer
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.presentation.OdrOdpReader
import com.engboost.monitoringdevices.core.document.impl.office.presentation.OdrPptReader
import com.engboost.monitoringdevices.core.document.impl.office.presentation.OdrPptxReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrCsvReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrOdsReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrXlsReader
import com.engboost.monitoringdevices.core.document.impl.office.spreadsheet.OdrXlsxReader
import com.engboost.monitoringdevices.core.document.impl.office.word.ChartRasterizingDocxReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrDocReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrDocxReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrOdtReader
import com.engboost.monitoringdevices.core.document.impl.office.word.OdrRtfReader

/**
 * Builds the reader sets. Both variants share every reader except DOCX.
 *
 * OpenDocument.core needs API 26: on older devices its classes are never loaded and
 * office formats are simply unsupported.
 */
internal class DefaultDocumentReaderVariants(
    converter: OdrHtmlConverter,
    rasterizer: DocxChartRasterizer
) : DocumentReaderVariants {

    private val isOfficeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private val sharedReaders: List<DocumentReader<Document>> = if (isOfficeSupported) {
        listOf(
            OdrDocReader(converter),
            OdrOdtReader(converter),
            OdrRtfReader(converter),
            OdrXlsxReader(converter),
            OdrXlsReader(converter),
            OdrOdsReader(converter),
            OdrCsvReader(converter),
            OdrPptxReader(converter),
            OdrPptReader(converter),
            OdrOdpReader(converter)
        )
    } else {
        emptyList()
    }

    private val readers: Map<ChartRendering, UniversalDocumentReader> = mapOf(
        ChartRendering.AS_IS to UniversalDocumentReader(
            sharedReaders + listOfNotNull(OdrDocxReader(converter).takeIf { isOfficeSupported })
        ),
        ChartRendering.AS_PICTURE to UniversalDocumentReader(
            sharedReaders + listOfNotNull(ChartRasterizingDocxReader(converter, rasterizer).takeIf { isOfficeSupported })
        )
    )

    override val formatsWithVariants: Set<DocumentFormat> =
        if (isOfficeSupported) setOf(DocumentFormat.DOCX) else emptySet()

    override fun reader(chartRendering: ChartRendering): UniversalDocumentReader = readers.getValue(chartRendering)
}
