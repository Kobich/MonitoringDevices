package com.engboost.monitoringdevices.core.document.api.format.audio

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.AudioDocument
import com.engboost.monitoringdevices.core.document.api.format.FormatReader

/** Audio (mp3, wav, m4a, aac, ogg, opus, flac): duration, tags, cover, technical info. */
interface AudioReader : FormatReader<AudioDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.AUDIO)
}
