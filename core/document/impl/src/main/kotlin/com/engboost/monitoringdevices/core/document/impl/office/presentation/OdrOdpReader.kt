package com.engboost.monitoringdevices.core.document.impl.office.presentation

import com.engboost.monitoringdevices.core.document.api.reader.presentation.OdpReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** ODP via OpenDocument.core. */
internal class OdrOdpReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), OdpReader
