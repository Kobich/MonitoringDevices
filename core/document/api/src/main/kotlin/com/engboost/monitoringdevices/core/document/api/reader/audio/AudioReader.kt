package com.engboost.monitoringdevices.core.document.api.reader.audio

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.document.AudioDocument
import com.engboost.monitoringdevices.core.document.api.reader.DocumentReader

/** Audio (mp3, wav, m4a, aac, ogg, opus, flac): duration, tags, cover, technical info. */
interface AudioReader : DocumentReader<AudioDocument> {
    override val formats: Set<DocumentFormat>
        get() = setOf(DocumentFormat.AUDIO)
}
