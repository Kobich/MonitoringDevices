package com.engboost.monitoringdevices.core.document.api.reader.text

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/**
 * Plain text (.txt, .md, .json, .xml, .log). The charset is detected (BOM, UTF-8 validation,
 * Windows-1251 / ISO-8859-1 fallback); long files are cut, see [TextDocument.isTruncated].
 */
interface TextReader : DocumentReader<TextDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.TEXT)
}
