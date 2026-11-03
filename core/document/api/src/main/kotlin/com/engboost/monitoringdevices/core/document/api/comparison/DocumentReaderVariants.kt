package com.engboost.monitoringdevices.core.document.api.comparison

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.reader.UniversalDocumentReader

/**
 * TEMPORARY: side-by-side comparison of how charts in office documents are shown.
 * Remove this file (and the losing variant) once one approach is chosen.
 */
enum class ChartRendering {
    /** The document goes to the renderer unchanged. */
    AS_IS,

    /** Charts are drawn by our code into pictures that replace them before rendering. */
    AS_PICTURE
}

interface DocumentReaderVariants {
    /** Formats whose output differs between [ChartRendering] variants. */
    val formatsWithVariants: Set<DocumentFormat>

    fun reader(chartRendering: ChartRendering): UniversalDocumentReader
}
