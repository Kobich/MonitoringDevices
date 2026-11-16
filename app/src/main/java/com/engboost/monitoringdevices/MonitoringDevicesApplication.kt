package com.engboost.monitoringdevices

import android.app.Application
import com.android.appmanager.feature.documentviewer.DocumentViewerFeatureApi
import com.android.appmanager.feature.filemanager.FileManagerFeatureApi
import com.android.appmanager.feature.nearbyscanner.NearbyScanner
import com.android.core.document.impl.DocumentCore
import com.engboost.monitoringdevices.sample.SampleFilesSeeder

/** Creates each feature once for the whole app. */
class MonitoringDevicesApplication : Application() {
    val scanner: NearbyScanner by lazy { NearbyScanner(this) }
    val fileManagerFeature: FileManagerFeatureApi by lazy { FileManagerFeatureApi(this) }
    val documentViewerFeature: DocumentViewerFeatureApi by lazy {
        DocumentViewerFeatureApi(this, DocumentCore(this).reader)
    }

    override fun onCreate() {
        super.onCreate()

        // TEMPORARY: example files for the file manager and the viewer, remove before release.
        Thread { SampleFilesSeeder(this).seedOnce() }.start()
    }
}
