package com.engboost.monitoringdevices.core.document.impl.office.spreadsheet

import com.engboost.monitoringdevices.core.document.api.format.spreadsheet.CsvReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** CSV via OpenDocument.core. */
internal class OdrCsvReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), CsvReader
