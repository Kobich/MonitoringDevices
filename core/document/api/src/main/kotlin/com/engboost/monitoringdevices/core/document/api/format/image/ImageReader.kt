package com.engboost.monitoringdevices.core.document.api.format.image

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Raster images (png, jpeg, gif, webp, bmp, heif). */
interface ImageReader : FormatReader<ImageDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.IMAGE)
}
