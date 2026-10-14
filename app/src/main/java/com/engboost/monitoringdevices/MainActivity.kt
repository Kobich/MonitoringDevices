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
import com.engboost.monitoringdevices.scanner.NearbyScanner
import com.engboost.monitoringdevices.scanner.ui.bluetooth.BluetoothScanScreen
import com.engboost.monitoringdevices.scanner.ui.wifi.WifiScanScreen
import com.engboost.monitoringdevices.ui.theme.MonitoringDevicesTheme

/** Tabs shown in the app; remove an entry to hide its scanner. */
private val enabledTabs = listOf(AppTab.WIFI, AppTab.BLUETOOTH)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanner = (application as MonitoringDevicesApplication).scanner
        enableEdgeToEdge()
        setContent {
            MonitoringDevicesTheme {
                AppNavigation(scanner = scanner, tabs = enabledTabs)
            }
        }
    }
}

@Composable
private fun AppNavigation(
    scanner: NearbyScanner,
    tabs: List<AppTab>
) {
    var selectedTab by rememberSaveable { mutableStateOf(tabs.first()) }

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
            AppTab.WIFI -> WifiScanScreen(scanner = scanner.wifi, modifier = modifier)
            AppTab.BLUETOOTH -> BluetoothScanScreen(scanner = scanner.bluetooth, modifier = modifier)
        }
    }
}

private enum class AppTab(
    val title: String,
    val shortTitle: String
) {
    WIFI("Wi-Fi", "Wi"),
    BLUETOOTH("Bluetooth", "Bt")
}
