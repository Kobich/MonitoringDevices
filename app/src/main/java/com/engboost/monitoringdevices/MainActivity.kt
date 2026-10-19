package com.engboost.monitoringdevices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.engboost.monitoringdevices.scanner.ui.NearbyScanScreen
import com.engboost.monitoringdevices.ui.theme.MonitoringDevicesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanner = (application as MonitoringDevicesApplication).scanner
        enableEdgeToEdge()
        setContent {
            MonitoringDevicesTheme {
                NearbyScanScreen(scanner = scanner)
            }
        }
    }
}
