package com.android.appmanager.feature.filemanager.di

import com.android.appmanager.feature.filemanager.FileManagerFeatureApi
import com.android.appmanager.feature.filemanager.FileManagerFeatureImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FileManagerFeatureModule {

    @Binds
    @Singleton
    abstract fun bindFeatureApi(impl: FileManagerFeatureImpl): FileManagerFeatureApi
}
