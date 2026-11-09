package com.android.appmanager.feature.documentviewer.impl.di

import com.android.appmanager.feature.documentviewer.api.DocumentViewerFeatureApi
import com.android.appmanager.feature.documentviewer.impl.DocumentViewerFeatureImpl
import com.android.appmanager.feature.documentviewer.impl.data.player.MediaPlayerAudioPlayerFactory
import com.android.appmanager.feature.documentviewer.impl.domain.player.AudioPlayerFactory
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
