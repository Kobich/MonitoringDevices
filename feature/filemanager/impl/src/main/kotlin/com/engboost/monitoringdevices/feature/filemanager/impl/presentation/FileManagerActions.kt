package com.engboost.monitoringdevices.feature.filemanager.impl.presentation

import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.BreadcrumbUi
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi

internal interface FileManagerActions {
    fun onBreadcrumbClick(breadcrumb: BreadcrumbUi)
    fun onEntryClick(entry: FileEntryUi)
    fun onEntryLongClick(entry: FileEntryUi)

    fun onImportFilesClick()
    fun onImportFolderClick()
    fun onCreateDirectoryClick()

    fun onSelectionClose()
    fun onSelectAllClick()
    fun onExportSelectedClick()
    fun onDeleteSelectedClick()

    fun onDeleteConfirm()
    fun onDeleteDismiss()
    fun onCreateDirectoryConfirm(name: String)
    fun onCreateDirectoryDismiss()
}
