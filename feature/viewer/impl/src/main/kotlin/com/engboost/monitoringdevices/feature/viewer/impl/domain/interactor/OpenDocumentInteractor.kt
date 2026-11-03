package com.engboost.monitoringdevices.feature.viewer.impl.domain.interactor

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.core.document.api.comparison.DocumentReaderVariants
import com.engboost.monitoringdevices.core.document.api.document.Document
import java.io.File

internal class OpenDocumentInteractor(
    // TEMPORARY: after the chart comparison this becomes a plain UniversalDocumentReader.
    private val readerVariants: DocumentReaderVariants
) {
    /** Whether the user can switch [ChartRendering] for this file. */
    fun hasChartVariants(file: File): Boolean {
        return DocumentFormat.of(file.name) in readerVariants.formatsWithVariants
    }

    suspend fun open(file: File, chartRendering: ChartRendering, password: String?): Document {
        return readerVariants.reader(chartRendering)
            .read(DocumentSource.LocalFile(file), ReadOptions(password = password))
    }
}
