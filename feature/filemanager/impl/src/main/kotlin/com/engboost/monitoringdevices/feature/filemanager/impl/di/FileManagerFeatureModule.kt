package com.engboost.monitoringdevices.feature.filemanager.impl.di

import android.content.Context
import com.engboost.monitoringdevices.feature.filemanager.api.FileManagerFeatureApi
import com.engboost.monitoringdevices.feature.filemanager.impl.FileManagerFeatureImpl
import com.engboost.monitoringdevices.feature.filemanager.impl.data.source.InternalFileSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FileManagerFeatureModule {

    @Binds
    @Singleton
    abstract fun bindFeatureApi(impl: FileManagerFeatureImpl): FileManagerFeatureApi

    companion object {
        /** Not `Context.dataDir`: it would also expose databases/, shared_prefs/ and cache/. */
        @Provides
        @Singleton
        fun provideInternalFileSource(@ApplicationContext context: Context): InternalFileSource {
            return InternalFileSource(rootDirectory = context.filesDir)
        }
    }
}
