package com.engboost.monitoringdevices.feature.wifi.impl.presentation.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiNetworkUi
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WifiScreen(
    state: WifiUiState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onNetworkClick: (WifiNetworkUi) -> Unit,
    onNetworkDetailsDismiss: () -> Unit,
    onOpenDeveloperOptionsClick: () -> Unit,
    onScanThrottlingDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.selectedNetwork?.let { network ->
        ModalBottomSheet(onDismissRequest = onNetworkDetailsDismiss) {
            WifiNetworkDetailsSheet(network = network)
        }
    }

    if (state.isScanThrottlingDialogVisible) {
        WifiScanThrottlingDialog(
            onOpenDeveloperOptionsClick = onOpenDeveloperOptionsClick,
            onDismiss = onScanThrottlingDialogDismiss
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "Wi-Fi", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            StatusCard(title = "Status", value = state.statusText)
        }
        item {
            StatusCard(title = "Found networks", value = state.networks.size.toString())
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
        if (state.networks.isEmpty()) {
            item {
                StatusCard(title = "Networks", value = "No networks found")
            }
        } else {
            items(
                items = state.networks,
                key = { network -> network.bssid }
            ) { network ->
                WifiNetworkCard(
                    network = network,
                    onClick = { onNetworkClick(network) }
                )
            }
        }
    }
}

@Composable
private fun WifiScanThrottlingDialog(
    onOpenDeveloperOptionsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Wi-Fi scan throttling is enabled") },
        text = {
            Text(
                text = "Android limits Wi-Fi scan frequency. Current refresh interval is 30 seconds. For local testing you can disable Wi-Fi scan throttling in Developer options."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenDeveloperOptionsClick) {
                Text(text = "Open Developer options")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK")
            }
        }
    )
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
