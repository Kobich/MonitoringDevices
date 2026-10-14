package com.engboost.monitoringdevices.monitoring.ui.bluetooth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.monitoring.ui.common.DetailRow

@Composable
internal fun BluetoothDeviceDetailsSheet(
    device: BluetoothDeviceUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Device details", style = MaterialTheme.typography.headlineSmall)
        DetailRow(title = "Name", value = device.name)
        DetailRow(title = "Address", value = device.address)
        DetailRow(title = "RSSI", value = "${device.rssiDbm} dBm")
        DetailRow(title = "Type", value = device.type)
        DetailRow(title = "Bond state", value = device.bondState)
    }
}
