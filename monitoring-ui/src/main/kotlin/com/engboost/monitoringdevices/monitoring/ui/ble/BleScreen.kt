package com.engboost.monitoringdevices.monitoring.ui.ble

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BleScreen(
    state: BleUiState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSortModeChange: (BleSortMode) -> Unit,
    onDeviceClick: (BleDeviceUi) -> Unit,
    onDeviceDetailsDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.selectedDevice?.let { device ->
        ModalBottomSheet(onDismissRequest = onDeviceDetailsDismiss) {
            BleDeviceDetailsSheet(device = device)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "Bluetooth LE", style = MaterialTheme.typography.headlineMedium)
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
                BleDeviceCard(
                    device = device,
                    onClick = { onDeviceClick(device) }
                )
            }
        }
    }
}

@Composable
private fun SortModeMenu(
    selectedMode: BleSortMode,
    onModeChange: (BleSortMode) -> Unit,
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
            BleSortMode.entries.forEach { mode ->
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

private fun BleSortMode.toDisplayName(): String {
    return when (this) {
        BleSortMode.STABLE -> "Stable"
        BleSortMode.SIGNAL -> "Signal strength"
        BleSortMode.NAME -> "Name"
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
