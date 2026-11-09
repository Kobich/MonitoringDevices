package com.android.appmanager.feature.documentviewer.di

import com.android.appmanager.feature.documentviewer.DocumentViewerFeatureApi
import com.android.appmanager.feature.documentviewer.DocumentViewerFeatureImpl
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
}
