package com.engboost.monitoringdevices.core.document.impl.office.spreadsheet

import com.engboost.monitoringdevices.core.document.api.format.spreadsheet.XlsxReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** XLSX via OpenDocument.core. */
internal class OdrXlsxReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), XlsxReader
