package com.engboost.monitoringdevices.feature.wifi.impl.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiNetworkUi

@Composable
internal fun WifiNetworkDetailsSheet(
    network: WifiNetworkUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Network details", style = MaterialTheme.typography.headlineSmall)
        DetailRow(title = "SSID", value = network.ssid)
        DetailRow(title = "BSSID", value = network.bssid)
        DetailRow(title = "RSSI", value = "${network.rssiDbm} dBm")
        DetailRow(title = "Frequency", value = "${network.frequencyMhz} MHz")
        DetailRow(title = "Capabilities", value = network.capabilities)
    }
}

@Composable
private fun DetailRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
