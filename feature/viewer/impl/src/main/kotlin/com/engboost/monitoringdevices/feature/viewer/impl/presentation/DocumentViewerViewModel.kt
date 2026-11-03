package com.engboost.monitoringdevices.feature.viewer.impl.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.core.document.api.document.Document
import com.engboost.monitoringdevices.core.document.api.document.HtmlDocument
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument
import com.engboost.monitoringdevices.core.document.api.document.PdfDocument
import com.engboost.monitoringdevices.core.document.api.document.TextDocument
import com.engboost.monitoringdevices.feature.viewer.impl.domain.interactor.OpenDocumentInteractor
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.DocumentViewerUiState
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.PasswordDialogUi
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.ViewerContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

internal class DocumentViewerViewModel(
    private val file: File,
    private val interactor: OpenDocumentInteractor
) : ViewModel() {
    private var loadJob: Job? = null
    private var openedDocument: Document? = null
    private var password: String? = null

    var uiState by mutableStateOf(
        DocumentViewerUiState(
            title = file.name,
            content = ViewerContent.Loading,
            chartRendering = ChartRendering.AS_IS.takeIf { interactor.hasChartVariants(file) },
            passwordDialog = null
        )
    )
        private set

    init {
        load()
    }

    fun onChartRenderingSelected(chartRendering: ChartRendering) {
        if (uiState.chartRendering == null || uiState.chartRendering == chartRendering) return
        uiState = uiState.copy(chartRendering = chartRendering)
        load()
    }

    fun onPageSelected(index: Int) {
        val content = uiState.content as? ViewerContent.Html ?: return
        uiState = uiState.copy(content = content.copy(selectedPageIndex = index))
    }

    fun onPasswordSubmit(value: String) {
        password = value
        uiState = uiState.copy(passwordDialog = null)
        load()
    }

    fun onPasswordDismiss() {
        uiState = uiState.copy(
            passwordDialog = null,
            content = ViewerContent.Error("The document is protected with a password")
        )
    }

    fun onRetryClick() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        uiState = uiState.copy(content = ViewerContent.Loading)
        val chartRendering = uiState.chartRendering ?: ChartRendering.AS_IS

        loadJob = viewModelScope.launch {
            try {
                val document = interactor.open(file, chartRendering, password)
                replaceOpenedDocument(document)
                uiState = uiState.copy(content = document.toContent())
            } catch (error: CancellationException) {
                throw error
            } catch (error: DocumentReadException) {
                handleReadError(error)
            } catch (error: Exception) {
                uiState = uiState.copy(content = ViewerContent.Error(error.toDisplayMessage()))
            }
        }
    }

    private fun handleReadError(error: DocumentReadException) {
        when (error.reason) {
            Reason.PASSWORD_REQUIRED -> uiState = uiState.copy(passwordDialog = PasswordDialogUi(error = null))
            Reason.INVALID_PASSWORD -> uiState = uiState.copy(passwordDialog = PasswordDialogUi(error = "Wrong password"))
            Reason.NOT_FOUND -> showError("File not found")
            Reason.UNSUPPORTED_FORMAT -> showError("This file type isn't supported")
            Reason.CORRUPTED -> showError("The file is damaged or has an unknown structure")
            Reason.TOO_LARGE -> showError("The file is too large")
            Reason.IO_ERROR -> showError("Can't read the file: ${error.toDisplayMessage()}")
        }
    }

    private fun showError(message: String) {
        uiState = uiState.copy(content = ViewerContent.Error(message))
    }

    private fun Document.toContent(): ViewerContent {
        // Exhaustive: a new document kind must be handled here.
        return when (this) {
            is HtmlDocument -> ViewerContent.Html(document = this, selectedPageIndex = 0)
            is PdfDocument -> ViewerContent.NotImplemented("PDF")
            is TextDocument -> ViewerContent.NotImplemented("text")
            is ImageDocument -> ViewerContent.NotImplemented("image")
        }
    }

    private fun replaceOpenedDocument(document: Document) {
        openedDocument?.takeIf { it !== document }?.close()
        openedDocument = document
    }

    override fun onCleared() {
        openedDocument?.close()
        openedDocument = null
    }

    private fun Throwable.toDisplayMessage(): String {
        return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
    }
}
