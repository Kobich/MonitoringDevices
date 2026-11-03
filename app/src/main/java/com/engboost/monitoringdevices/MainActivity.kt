package com.engboost.monitoringdevices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.engboost.monitoringdevices.core.commonui.theme.MonitoringDevicesTheme
import com.engboost.monitoringdevices.feature.filemanager.api.FileManagerFeatureApi
import com.engboost.monitoringdevices.feature.viewer.api.DocumentViewerFeatureApi
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
        enableEdgeToEdge()
        setContent {
            MonitoringDevicesTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        fileManagerFeature = fileManagerFeature,
                        documentViewerFeature = documentViewerFeature
                    )
                }
            }
        }
    }
}
