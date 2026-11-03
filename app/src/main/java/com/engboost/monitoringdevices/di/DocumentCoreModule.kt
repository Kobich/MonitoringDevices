package com.engboost.monitoringdevices.di

import android.content.Context
import com.engboost.monitoringdevices.core.document.api.DocumentReader
import com.engboost.monitoringdevices.core.document.api.cache.RenderedDocumentCache
import com.engboost.monitoringdevices.core.document.impl.DocumentCore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** `:core:document` has no DI framework: the app builds it once and exposes its API types. */
@Module
@InstallIn(SingletonComponent::class)
object DocumentCoreModule {

    @Provides
    @Singleton
    fun provideDocumentCore(@ApplicationContext context: Context): DocumentCore = DocumentCore(context)

    @Provides
    fun provideDocumentReader(core: DocumentCore): DocumentReader = core.reader

    @Provides
    fun provideRenderedDocumentCache(core: DocumentCore): RenderedDocumentCache = core.cache
}
