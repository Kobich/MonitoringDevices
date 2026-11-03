package com.engboost.monitoringdevices.feature.viewer.impl.domain.interactor

import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.reader.UniversalDocumentReader
import java.io.File
import javax.inject.Inject

internal class OpenDocumentInteractor @Inject constructor(
    private val reader: UniversalDocumentReader
) {
    suspend fun open(file: File, password: String?): Document {
        return reader.read(DocumentSource.LocalFile(file), ReadOptions(password = password))
    }
}
