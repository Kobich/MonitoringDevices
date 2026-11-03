package com.engboost.monitoringdevices.feature.viewer.impl.domain.interactor

import com.engboost.monitoringdevices.core.document.api.DocumentReader
import com.engboost.monitoringdevices.core.document.api.document.Document
import java.io.File
import javax.inject.Inject

internal class OpenDocumentInteractor @Inject constructor(
    private val reader: DocumentReader
) {
    suspend fun open(file: File, password: String?): Document = reader.read(file, password)
}
