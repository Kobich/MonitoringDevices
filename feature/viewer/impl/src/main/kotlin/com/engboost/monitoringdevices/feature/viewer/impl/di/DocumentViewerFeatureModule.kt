package com.engboost.monitoringdevices.feature.viewer.impl.di

import com.engboost.monitoringdevices.feature.viewer.api.DocumentViewerFeatureApi
import com.engboost.monitoringdevices.feature.viewer.impl.DocumentViewerFeatureImpl
import com.engboost.monitoringdevices.feature.viewer.impl.domain.interactor.OpenDocumentInteractor
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.DocumentViewerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val documentViewerFeatureModule = module {
    single<DocumentViewerFeatureApi> { DocumentViewerFeatureImpl() }
    factory { OpenDocumentInteractor(get()) }
    viewModel { parameters -> DocumentViewerViewModel(file = parameters.get(), interactor = get()) }
}
