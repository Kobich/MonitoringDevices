package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothDeviceUi
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothSortMode
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BluetoothScreen(
    state: BluetoothUiState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSortModeChange: (BluetoothSortMode) -> Unit,
    onDeviceClick: (BluetoothDeviceUi) -> Unit,
    onDeviceDetailsDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.selectedDevice?.let { device ->
        ModalBottomSheet(onDismissRequest = onDeviceDetailsDismiss) {
            BluetoothDeviceDetailsSheet(device = device)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "Bluetooth", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            StatusCard(title = "Status", value = state.statusText)
        }
        item {
            StatusCard(title = "Found devices", value = state.devices.size.toString())
        }
        item {
            SortModeMenu(
                selectedMode = state.sortMode,
                onModeChange = onSortModeChange
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartClick,
                    enabled = !state.isScanning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Start")
                }
                OutlinedButton(
                    onClick = onStopClick,
                    enabled = state.isScanning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Stop")
                }
            }
        }
        if (state.devices.isEmpty()) {
            item {
                StatusCard(title = "Devices", value = "No devices found")
            }
        } else {
            items(
                items = state.devices,
                key = { device -> device.address }
            ) { device ->
                BluetoothDeviceCard(
                    device = device,
                    onClick = { onDeviceClick(device) }
                )
            }
        }
    }
}

@Composable
private fun SortModeMenu(
    selectedMode: BluetoothSortMode,
    onModeChange: (BluetoothSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { isExpanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Sort: ${selectedMode.toDisplayName()}")
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            BluetoothSortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.toDisplayName()) },
                    onClick = {
                        isExpanded = false
                        onModeChange(mode)
                    }
                )
            }
        }
    }
}

private fun BluetoothSortMode.toDisplayName(): String {
    return when (this) {
        BluetoothSortMode.STABLE -> "Stable"
        BluetoothSortMode.SIGNAL -> "Signal strength"
        BluetoothSortMode.NAME -> "Name"
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
