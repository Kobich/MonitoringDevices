package com.engboost.monitoringdevices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.monitoring.DeviceMonitoring
import com.engboost.monitoringdevices.monitoring.MonitoringSource
import com.engboost.monitoringdevices.monitoring.ui.bluetooth.BluetoothMonitoringScreen
import com.engboost.monitoringdevices.monitoring.ui.wifi.WifiMonitoringScreen
import com.engboost.monitoringdevices.ui.theme.MonitoringDevicesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val monitoring = DeviceMonitoring.get(this)
        enableEdgeToEdge()
        setContent {
            MonitoringDevicesTheme {
                AppNavigation(monitoring = monitoring)
            }
        }
    }
}

@Composable
private fun AppNavigation(monitoring: DeviceMonitoring) {
    val tabs = AppTab.entries.filter { tab -> tab.source in monitoring.config.enabledSources }
    var selectedTab by rememberSaveable { mutableStateOf(tabs.firstOrNull()) }

    Scaffold(
        bottomBar = {
            if (tabs.size > 1) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(text = tab.title) },
                            icon = { Text(text = tab.shortTitle) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (selectedTab) {
            AppTab.WIFI -> WifiMonitoringScreen(monitoring = monitoring, modifier = modifier)
            AppTab.BLUETOOTH -> BluetoothMonitoringScreen(monitoring = monitoring, modifier = modifier)
            null -> Text(text = "No monitoring sources enabled", modifier = modifier)
        }
    }
}

private enum class AppTab(
    val source: MonitoringSource,
    val title: String,
    val shortTitle: String
) {
    WIFI(MonitoringSource.WIFI, "Wi-Fi", "Wi"),
    BLUETOOTH(MonitoringSource.BLUETOOTH, "Bluetooth", "Bt")
}
