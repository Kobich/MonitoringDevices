package com.engboost.monitoringdevices.core.document.impl.office.spreadsheet

import com.engboost.monitoringdevices.core.document.api.reader.spreadsheet.XlsReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** XLS via OpenDocument.core. */
internal class OdrXlsReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), XlsReader
