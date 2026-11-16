package com.android.core.fileaccess.di

import com.android.core.fileaccess.FileAccessConfigSource
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Lets the app bind no source at all; `Optional` would need API 24. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class FileAccessModule {
    @Multibinds
    abstract fun configSources(): Set<FileAccessConfigSource>
}
