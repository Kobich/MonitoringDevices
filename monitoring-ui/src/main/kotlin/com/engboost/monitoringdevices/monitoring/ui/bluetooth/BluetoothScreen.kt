package com.engboost.monitoringdevices.monitoring.ui.bluetooth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.engboost.monitoringdevices.monitoring.ui.common.StartStopButtons
import com.engboost.monitoringdevices.monitoring.ui.common.StatusCard

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
            StartStopButtons(
                isRunning = state.isScanning,
                onStartClick = onStartClick,
                onStopClick = onStopClick
            )
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
            Text(text = "Sort: ${selectedMode.title}")
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            BluetoothSortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.title) },
                    onClick = {
                        isExpanded = false
                        onModeChange(mode)
                    }
                )
            }
        }
    }
}
