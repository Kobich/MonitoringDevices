package com.engboost.monitoringdevices.core.document.impl.office.drawing

import com.engboost.monitoringdevices.core.document.api.reader.drawing.OdgReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** OpenDocument drawing via OpenDocument.core. */
internal class OdrOdgReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), OdgReader
