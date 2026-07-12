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
import com.engboost.monitoringdevices.feature.bluetooth.api.BluetoothFeatureApi
import com.engboost.monitoringdevices.feature.radio.api.RadioFeatureApi
import com.engboost.monitoringdevices.feature.wifi.api.WifiFeatureApi
import com.engboost.monitoringdevices.ui.theme.MonitoringDevicesTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {
    private val wifiFeature: WifiFeatureApi by inject()
    private val bluetoothFeature: BluetoothFeatureApi by inject()
    private val radioFeature: RadioFeatureApi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonitoringDevicesTheme {
                AppNavigation(
                    wifiFeature = wifiFeature,
                    bluetoothFeature = bluetoothFeature,
                    radioFeature = radioFeature
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(
    wifiFeature: WifiFeatureApi,
    bluetoothFeature: BluetoothFeatureApi,
    radioFeature: RadioFeatureApi
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.WIFI) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
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
        val modifier = Modifier.padding(innerPadding)

        when (selectedTab) {
            AppTab.WIFI -> wifiFeature.WifiScreen(modifier = modifier)
            AppTab.BLUETOOTH -> bluetoothFeature.BluetoothScreen(modifier = modifier)
            AppTab.RADIO -> radioFeature.RadioScreen(modifier = modifier)
        }
    }
}

private enum class AppTab(
    val title: String,
    val shortTitle: String
) {
    WIFI("Wi-Fi", "Wi"),
    BLUETOOTH("Bluetooth", "Bt"),
    RADIO("Radio", "Ra")
}
