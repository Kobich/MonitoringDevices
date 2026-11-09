package com.android.appmanager.feature.viewer.impl.di

import com.android.appmanager.feature.viewer.api.DocumentViewerFeatureApi
import com.android.appmanager.feature.viewer.impl.DocumentViewerFeatureImpl
import com.android.appmanager.feature.viewer.impl.data.player.MediaPlayerAudioPlayerFactory
import com.android.appmanager.feature.viewer.impl.domain.player.AudioPlayerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Needs `DocumentReader` from the host app's graph, see `DocumentCoreModule` in `:app`. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DocumentViewerFeatureModule {

    @Binds
    @Singleton
    abstract fun bindFeatureApi(impl: DocumentViewerFeatureImpl): DocumentViewerFeatureApi

    @Binds
    abstract fun bindAudioPlayerFactory(impl: MediaPlayerAudioPlayerFactory): AudioPlayerFactory
}
