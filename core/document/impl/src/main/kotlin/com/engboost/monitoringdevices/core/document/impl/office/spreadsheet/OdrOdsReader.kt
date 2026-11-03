package com.engboost.monitoringdevices.core.document.impl.office.spreadsheet

import com.engboost.monitoringdevices.core.document.api.reader.spreadsheet.OdsReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** ODS via OpenDocument.core. */
internal class OdrOdsReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), OdsReader
