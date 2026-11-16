package com.engboost.monitoringdevices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.android.core.commonui.theme.AppTheme
import com.engboost.monitoringdevices.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MonitoringDevicesApplication
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        fileManagerFeature = app.fileManagerFeature,
                        documentViewerFeature = app.documentViewerFeature,
                        scanner = app.scanner
                    )
                }
            }
        }
    }
}
