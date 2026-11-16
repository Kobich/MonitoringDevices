package com.android.appmanager.feature.filemanager.di

import android.content.Context
import com.android.appmanager.feature.filemanager.FileManagerFeatureApi
import com.android.appmanager.feature.filemanager.FileManagerFeatureImpl
import com.android.appmanager.feature.filemanager.data.source.InternalFileSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FileManagerFeatureModule {

    @Binds
    @Singleton
    abstract fun bindFeatureApi(impl: FileManagerFeatureImpl): FileManagerFeatureApi

    companion object {
        /** Also in `SampleFilesSeeder` of `:app`. */
        private const val STORAGE_DIRECTORY = "storage"

        /**
         * A folder of its own in `filesDir`: libraries keep files there too (e.g. androidx.profileinstaller's
         * `profileInstalled`), and the user shouldn't see them. Not `Context.dataDir`: it would also expose
         * databases/, shared_prefs/ and cache/.
         */
        @Provides
        @Singleton
        fun provideInternalFileSource(@ApplicationContext context: Context): InternalFileSource {
            return InternalFileSource(rootDirectory = File(context.filesDir, STORAGE_DIRECTORY))
        }
    }
}
