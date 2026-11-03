package com.engboost.monitoringdevices.core.document.impl.office

import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/**
 * Base of office readers backed by OpenDocument.core. A concrete reader only names
 * its API interface (e.g. `DocxReader`), which fixes the format.
 */
internal abstract class OdrOfficeReader(
    protected val converter: OdrHtmlConverter
) : DocumentReader<HtmlDocument> {

    override suspend fun read(source: DocumentSource, options: ReadOptions): HtmlDocument {
        return converter.convert(
            source = source,
            format = formats.single(),
            password = options.password,
            variant = VARIANT
        )
    }

    private companion object {
        const val VARIANT = "as-is"
    }
}
