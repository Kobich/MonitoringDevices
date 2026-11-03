package com.engboost.monitoringdevices.core.document.api.format.text

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/**
 * Plain text (.txt, .json, .xml, .log). The charset is detected (BOM, UTF-8 validation,
 * Windows-1251 / ISO-8859-1 fallback); long files are cut, see [TextDocument.isTruncated].
 */
interface TextReader : FormatReader<TextDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.TEXT)
}
