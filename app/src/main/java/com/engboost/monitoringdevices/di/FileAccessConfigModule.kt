package com.engboost.monitoringdevices.di

import com.android.core.fileaccess.FileAccessConfigSource
import com.engboost.monitoringdevices.config.MockFileAccessConfigSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** TEMPORARY: the mock until the store module gives the config. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class FileAccessConfigModule {
    @Binds
    @IntoSet
    abstract fun bindConfigSource(source: MockFileAccessConfigSource): FileAccessConfigSource
}
