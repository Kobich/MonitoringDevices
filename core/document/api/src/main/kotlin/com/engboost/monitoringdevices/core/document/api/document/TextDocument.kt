package com.engboost.monitoringdevices.core.document.api.document

import com.engboost.monitoringdevices.core.document.api.DocumentFormat

data class TextDocument(
    override val name: String,
    override val format: DocumentFormat,
    val text: String,
    val charsetName: String,
    val isTruncated: Boolean
) : Document
