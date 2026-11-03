package com.engboost.monitoringdevices.feature.viewer.impl.di

import com.engboost.monitoringdevices.feature.viewer.api.DocumentViewerFeatureApi
import com.engboost.monitoringdevices.feature.viewer.impl.DocumentViewerFeatureImpl
import com.engboost.monitoringdevices.feature.viewer.impl.data.player.MediaPlayerAudioPlayerFactory
import com.engboost.monitoringdevices.feature.viewer.impl.domain.player.AudioPlayerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Needs `DocumentReaderVariants` (`:core:document:api`) from the host app's graph,
 * see `DocumentCoreModule` in `:app`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DocumentViewerFeatureModule {

    @Binds
    @Singleton
    abstract fun bindFeatureApi(impl: DocumentViewerFeatureImpl): DocumentViewerFeatureApi

    @Binds
    abstract fun bindAudioPlayerFactory(impl: MediaPlayerAudioPlayerFactory): AudioPlayerFactory
}
