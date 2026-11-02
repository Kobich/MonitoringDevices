package com.engboost.monitoringdevices

import android.app.Application
import com.engboost.monitoringdevices.feature.filemanager.impl.di.fileManagerFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MonitoringDevicesApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MonitoringDevicesApplication)
            modules(
                fileManagerFeatureModule
            )
        }
    }
}
