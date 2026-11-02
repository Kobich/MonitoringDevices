package com.engboost.monitoringdevices.feature.filemanager.impl.di

import com.engboost.monitoringdevices.feature.filemanager.api.FileManagerFeatureApi
import com.engboost.monitoringdevices.feature.filemanager.impl.FileManagerFeatureImpl
import com.engboost.monitoringdevices.feature.filemanager.impl.data.repository.DefaultFileStorageRepository
import com.engboost.monitoringdevices.feature.filemanager.impl.data.source.ExternalDocumentSource
import com.engboost.monitoringdevices.feature.filemanager.impl.data.source.InternalFileSource
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.interactor.FileManagerInteractor
import com.engboost.monitoringdevices.feature.filemanager.impl.domain.repository.FileStorageRepository
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.FileManagerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val fileManagerFeatureModule = module {
    single<FileManagerFeatureApi> { FileManagerFeatureImpl() }

    // Root of the browsable storage. Context.dataDir would also expose
    // databases/, shared_prefs/ and cache/.
    single { InternalFileSource(rootDirectory = androidContext().filesDir) }
    single { ExternalDocumentSource(context = androidContext()) }
    single<FileStorageRepository> { DefaultFileStorageRepository(get(), get()) }

    factory { FileManagerInteractor(get()) }
    viewModel { FileManagerViewModel(get(), get()) }
}
