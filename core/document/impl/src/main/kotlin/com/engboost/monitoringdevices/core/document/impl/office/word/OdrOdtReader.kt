package com.engboost.monitoringdevices.core.document.impl.office.word

import com.engboost.monitoringdevices.core.document.api.reader.word.OdtReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** ODT via OpenDocument.core. */
internal class OdrOdtReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), OdtReader
