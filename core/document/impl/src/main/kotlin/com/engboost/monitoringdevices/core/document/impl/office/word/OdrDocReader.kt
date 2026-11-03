package com.engboost.monitoringdevices.core.document.impl.office.word

import com.engboost.monitoringdevices.core.document.api.reader.word.DocReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** DOC via OpenDocument.core. */
internal class OdrDocReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), DocReader
