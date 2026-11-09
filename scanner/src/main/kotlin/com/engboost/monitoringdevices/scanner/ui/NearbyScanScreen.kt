package com.engboost.monitoringdevices.scanner.ui

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

/** The host app opens this screen as a single destination; the tabs are internal to the feature. */
@Composable
fun NearbyScanScreen(
    scanner: NearbyScanner,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(ScanTab.WIFI) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                ScanTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(text = tab.title) },
                        icon = { Text(text = tab.shortTitle) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)

        when (selectedTab) {
            ScanTab.WIFI -> WifiScanScreen(scanner = scanner.wifi, modifier = contentModifier)
            ScanTab.BLUETOOTH -> BluetoothScanScreen(scanner = scanner.bluetooth, modifier = contentModifier)
        }
    }
}

private enum class ScanTab(
    val title: String,
    val shortTitle: String
) {
    WIFI("Wi-Fi", "Wi"),
    BLUETOOTH("Bluetooth", "Bt")
}
