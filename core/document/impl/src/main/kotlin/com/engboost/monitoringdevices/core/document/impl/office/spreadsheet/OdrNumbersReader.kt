package com.engboost.monitoringdevices.core.document.impl.office.spreadsheet

import com.engboost.monitoringdevices.core.document.api.reader.spreadsheet.NumbersReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** Apple Numbers via OpenDocument.core. */
internal class OdrNumbersReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), NumbersReader
