package com.engboost.monitoringdevices.di

import android.content.Context
import com.engboost.monitoringdevices.core.document.api.DocumentReader
import com.engboost.monitoringdevices.core.document.impl.DocumentCore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** `:core:document` has no DI framework: the app builds it once and exposes the reader. */
@Module
@InstallIn(SingletonComponent::class)
object DocumentCoreModule {

    @Provides
    @Singleton
    fun provideDocumentReader(@ApplicationContext context: Context): DocumentReader = DocumentCore(context).reader
}
