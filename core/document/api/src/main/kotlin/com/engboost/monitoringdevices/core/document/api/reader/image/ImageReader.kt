package com.engboost.monitoringdevices.core.document.api.reader.image

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** Raster images (png, jpeg, gif, webp, bmp, heif). */
interface ImageReader : DocumentReader<ImageDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.IMAGE)
}
