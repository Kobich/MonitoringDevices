package com.engboost.monitoringdevices.feature.viewer.impl.di

import com.engboost.monitoringdevices.feature.viewer.api.DocumentViewerFeatureApi
import com.engboost.monitoringdevices.feature.viewer.impl.DocumentViewerFeatureImpl
import com.engboost.monitoringdevices.feature.viewer.impl.data.player.MediaPlayerAudioPlayerFactory
import com.engboost.monitoringdevices.feature.viewer.impl.domain.player.AudioPlayerFactory
import com.engboost.monitoringdevices.feature.viewer.impl.domain.interactor.OpenDocumentInteractor
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.DocumentViewerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val documentViewerFeatureModule = module {
    single<DocumentViewerFeatureApi> { DocumentViewerFeatureImpl() }
    factory { OpenDocumentInteractor(get()) }
    single<AudioPlayerFactory> { MediaPlayerAudioPlayerFactory(androidContext()) }
    viewModel { parameters ->
        DocumentViewerViewModel(file = parameters.get(), interactor = get(), audioPlayerFactory = get())
    }
}
