package com.engboost.monitoringdevices.core.document.impl.office.presentation

import com.engboost.monitoringdevices.core.document.api.format.presentation.KeynoteReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** Apple Keynote via OpenDocument.core. */
internal class OdrKeynoteReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), KeynoteReader
