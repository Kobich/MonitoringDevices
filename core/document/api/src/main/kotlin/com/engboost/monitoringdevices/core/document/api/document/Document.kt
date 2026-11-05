package com.engboost.monitoringdevices.core.document.api.document

import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import java.io.Closeable

sealed interface Document : Closeable {
    val name: String
    val format: DocumentFormat

    override fun close() = Unit
}
