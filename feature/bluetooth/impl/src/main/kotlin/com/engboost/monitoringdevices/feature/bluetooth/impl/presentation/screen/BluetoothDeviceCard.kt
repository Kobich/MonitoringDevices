package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.screen

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
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothDeviceUi

@Composable
internal fun BluetoothDeviceCard(
    device: BluetoothDeviceUi,
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
            Text(text = device.name, style = MaterialTheme.typography.titleMedium)
            Text(text = device.address, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${device.rssiDbm.toRssiText()} / ${device.type}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun Int?.toRssiText(): String {
    return this?.let { rssi -> "$rssi dBm" } ?: "RSSI unavailable"
}
