package com.engboost.monitoringdevices.di

import android.content.Context
import com.android.core.document.api.DocumentReader
import com.android.core.document.impl.DocumentCore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DocumentCoreModule {

    @Provides
    @Singleton
    fun provideDocumentReader(@ApplicationContext context: Context): DocumentReader = DocumentCore(context).reader
}
