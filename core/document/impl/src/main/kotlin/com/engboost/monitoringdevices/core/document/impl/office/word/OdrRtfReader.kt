package com.engboost.monitoringdevices.core.document.impl.office.word

import com.engboost.monitoringdevices.core.document.api.reader.word.RtfReader
import com.engboost.monitoringdevices.core.document.impl.office.OdrHtmlConverter
import com.engboost.monitoringdevices.core.document.impl.office.OdrOfficeReader

/** RTF via OpenDocument.core. */
internal class OdrRtfReader(converter: OdrHtmlConverter) : OdrOfficeReader(converter), RtfReader
