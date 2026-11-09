package com.engboost.monitoringdevices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.android.appmanager.feature.filemanager.api.FileManagerFeatureApi
import com.android.appmanager.feature.viewer.api.DocumentViewerFeatureApi
import com.android.core.commonui.theme.MonitoringDevicesTheme
import com.engboost.monitoringdevices.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var fileManagerFeature: FileManagerFeatureApi

    @Inject
    lateinit var documentViewerFeature: DocumentViewerFeatureApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanner = (application as MonitoringDevicesApplication).scanner
        enableEdgeToEdge()
        setContent {
            MonitoringDevicesTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        fileManagerFeature = fileManagerFeature,
                        documentViewerFeature = documentViewerFeature,
                        scanner = scanner
                    )
                }
            }
        }
    }
}
