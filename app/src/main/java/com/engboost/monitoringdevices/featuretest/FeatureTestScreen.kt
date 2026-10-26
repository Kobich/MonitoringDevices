package com.engboost.monitoringdevices.featuretest

import android.Manifest
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.features.bluetooth.BluetoothConnectedDevicesFeature
import com.engboost.monitoringdevices.features.bluetooth.NearbyBluetoothDevicesFeature
import com.engboost.monitoringdevices.features.location.GeoZone
import com.engboost.monitoringdevices.features.location.LocationFeature
import com.engboost.monitoringdevices.features.time.TimeFeature
import com.engboost.monitoringdevices.features.time.TimePeriod
import com.engboost.monitoringdevices.features.wifi.WifiNetworksFeature
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT
)

/** Manual test screen: calls every feature separately and shows the raw result. */
@Composable
fun FeatureTestScreen() {
    val context = LocalContext.current.applicationContext
    val wifi = remember { WifiNetworksFeature(context) }
    val connectedBluetooth = remember { BluetoothConnectedDevicesFeature(context) }
    val nearbyBluetooth = remember { NearbyBluetoothDevicesFeature(context) }
    val location = remember { LocationFeature(context) }
    val time = remember { TimeFeature(context) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PermissionsCard()

            FeatureCard(title = "Wi-Fi networks") {
                var names by remember { mutableStateOf("") }
                NamesField(value = names, onValueChange = { names = it }, label = "Network names (SSID)")
                FeatureAction(label = "getData") { wifi.getData() }
                FeatureAction(label = "isMeetsCondition") { wifi.isMeetsCondition(names.toNames()) }
            }

            FeatureCard(title = "Connected Bluetooth devices") {
                var names by remember { mutableStateOf("") }
                NamesField(value = names, onValueChange = { names = it }, label = "Device names")
                FeatureAction(label = "getData") { connectedBluetooth.getData() }
                FeatureAction(label = "isMeetsCondition") { connectedBluetooth.isMeetsCondition(names.toNames()) }
            }

            FeatureCard(title = "Nearby Bluetooth devices") {
                Text(
                    text = "Up to 10 s per call, at most once per 30 s",
                    style = MaterialTheme.typography.bodySmall
                )
                var names by remember { mutableStateOf("") }
                NamesField(value = names, onValueChange = { names = it }, label = "Device names")
                FeatureAction(label = "getData") { nearbyBluetooth.getData() }
                FeatureAction(label = "isMeetsCondition") { nearbyBluetooth.isMeetsCondition(names.toNames()) }
            }

            FeatureCard(title = "Location") {
                Text(text = "Up to 20 s per call", style = MaterialTheme.typography.bodySmall)
                var zones by remember { mutableStateOf("55.7558, 37.6173, 100") }
                OutlinedTextField(
                    value = zones,
                    onValueChange = { zones = it },
                    label = { Text("Zones: latitude, longitude, radius in meters; one per line") },
                    modifier = Modifier.fillMaxWidth()
                )
                FeatureAction(label = "isMeetsCondition") { location.isMeetsCondition(zones.toZones()) }
            }

            FeatureCard(title = "Time") {
                var start by remember { mutableStateOf(nowShifted(hours = -1)) }
                var end by remember { mutableStateOf(nowShifted(hours = 1)) }
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start, ISO 8601") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End, ISO 8601") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FeatureAction(label = "getData") { time.getData() }
                FeatureAction(label = "isMeetsCondition") { time.isMeetsCondition(TimePeriod(start, end)) }
            }
        }
    }
}

@Composable
private fun PermissionsCard() {
    var result by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        result = granted.entries.joinToString("\n") { (permission, isGranted) ->
            "${permission.substringAfterLast('.')}: ${if (isGranted) "granted" else "denied"}"
        }
    }

    FeatureCard(title = "Permissions") {
        Button(onClick = { launcher.launch(PERMISSIONS) }) {
            Text(text = "Request permissions")
        }
        result?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun FeatureCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun NamesField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label, comma separated") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** A button that runs [action] and shows its result and how long it took. */
@Composable
private fun FeatureAction(label: String, action: suspend () -> Any?) {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Button(
        onClick = {
            isRunning = true
            scope.launch {
                val startMillis = SystemClock.elapsedRealtime()
                val text = try {
                    action().toDisplayText()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: IllegalArgumentException) {
                    "Invalid input: ${error.message}"
                } catch (error: Exception) {
                    "Error: ${error::class.simpleName}: ${error.message}"
                }
                val elapsedMillis = SystemClock.elapsedRealtime() - startMillis
                result = "$text\n($elapsedMillis ms)"
                isRunning = false
            }
        },
        enabled = !isRunning
    ) {
        Text(text = label)
    }
    if (isRunning) {
        CircularProgressIndicator()
    } else {
        result?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
    }
}

private fun Any?.toDisplayText(): String = when (this) {
    null -> "null"
    is List<*> -> if (isEmpty()) "empty list" else "$size items:\n" + joinToString("\n")
    else -> toString()
}

private fun String.toNames(): List<String> = split(',').map { it.trim() }.filter { it.isNotEmpty() }

private fun String.toZones(): List<GeoZone> = lines()
    .filter { it.isNotBlank() }
    .map { line ->
        val parts = line.split(',').map { it.trim().toDoubleOrNull() }
        require(parts.size == 3 && parts.all { it != null }) {
            "Each zone line must be: latitude, longitude, radius ($line)"
        }
        GeoZone(latitude = parts[0]!!, longitude = parts[1]!!, radiusMeters = parts[2]!!)
    }

private fun nowShifted(hours: Long): String {
    return ZonedDateTime.now()
        .plusHours(hours)
        .truncatedTo(ChronoUnit.SECONDS)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
