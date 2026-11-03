package com.engboost.monitoringdevices.core.document.impl.office.presentation

import com.engboost.monitoringdevices.core.document.api.reader.presentation.PptxReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** PPTX via OpenDocument.core. */
internal class OdrPptxReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), PptxReader
