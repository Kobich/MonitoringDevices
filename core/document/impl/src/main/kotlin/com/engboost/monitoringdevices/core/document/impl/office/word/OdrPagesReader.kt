package com.engboost.monitoringdevices.core.document.impl.office.word

import com.engboost.monitoringdevices.core.document.api.format.word.PagesReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** Apple Pages via OpenDocument.core. */
internal class OdrPagesReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), PagesReader
