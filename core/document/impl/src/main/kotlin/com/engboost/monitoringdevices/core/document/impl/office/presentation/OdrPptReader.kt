package com.engboost.monitoringdevices.core.document.impl.office.presentation

import com.engboost.monitoringdevices.core.document.api.reader.presentation.PptReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** PPT via OpenDocument.core. */
internal class OdrPptReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), PptReader
