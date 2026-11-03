package com.engboost.monitoringdevices

import android.app.Application
import com.engboost.monitoringdevices.core.document.impl.di.coreDocumentModule
import com.engboost.monitoringdevices.feature.filemanager.impl.di.fileManagerFeatureModule
import com.engboost.monitoringdevices.feature.viewer.impl.di.documentViewerFeatureModule
import com.engboost.monitoringdevices.sample.SampleFilesSeeder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MonitoringDevicesApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // TEMPORARY: example files for the file manager and the viewer, remove before release.
        Thread { SampleFilesSeeder(this).seedOnce() }.start()

        startKoin {
            androidLogger()
            androidContext(this@MonitoringDevicesApplication)
            modules(
                coreDocumentModule,
                fileManagerFeatureModule,
                documentViewerFeatureModule
            )
        }
    }
}
