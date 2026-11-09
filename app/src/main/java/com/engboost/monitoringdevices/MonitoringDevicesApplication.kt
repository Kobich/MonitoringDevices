package com.engboost.monitoringdevices

import android.app.Application
import com.android.appmanager.feature.nearbyscanner.NearbyScanner
import com.engboost.monitoringdevices.sample.SampleFilesSeeder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MonitoringDevicesApplication : Application() {
    val scanner: NearbyScanner by lazy { NearbyScanner(this) }

    override fun onCreate() {
        super.onCreate()

        // TEMPORARY: example files for the file manager and the viewer, remove before release.
        Thread { SampleFilesSeeder(this).seedOnce() }.start()
    }
}
