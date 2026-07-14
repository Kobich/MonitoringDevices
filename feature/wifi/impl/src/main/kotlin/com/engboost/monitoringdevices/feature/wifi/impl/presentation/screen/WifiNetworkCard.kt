package com.engboost.monitoringdevices.feature.wifi.impl.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiNetworkUi

@Composable
internal fun WifiNetworkCard(
    network: WifiNetworkUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = network.ssid, style = MaterialTheme.typography.titleMedium)
            Text(text = network.bssid, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${network.rssiDbm} dBm / ${network.frequencyMhz} MHz",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
