# scanner

## Description

Scans nearby Wi-Fi networks and Bluetooth devices and shows them on a ready-made screen. The feature is meant for a short foreground session: the user opens the screen, looks at the devices around and leaves; nothing runs in the background. It is a self-contained module that a host app adds as one destination of its Compose navigation graph.

## Architecture and stack

* **Layers:** Data (`wifi/`, `bluetooth/` — scanners on top of Android APIs) and Presentation (`ui/` — Compose screens and ViewModels). There is no separate Domain layer: scanners expose events that the UI reduces into state directly.
* **Key technologies:**
  * Kotlin 2.0.21, Kotlin Coroutines / Flow 1.9.0
  * Jetpack Compose (BOM 2024.09.00), Material 3
  * Navigation Compose 2.8.4
  * Lifecycle (ViewModel, `repeatOnLifecycle`, `collectAsStateWithLifecycle`) 2.8.7, Activity Compose 1.9.3
  * Android APIs: `WifiManager`, `BluetoothLeScanner`
  * Android Gradle Plugin 9.0.1, Gradle 9.2.1, Java 11
  * SDK: `minSdk` 24, `compileSdk` 36.1
* **Dependencies:** no `:core` or `:features` modules. External dependencies only: Kotlin Coroutines (API), Navigation Compose (API), Compose, Lifecycle and Activity Compose (implementation).

Package layout:

```text
com.engboost.monitoringdevices.scanner
├── NearbyScanner.kt / NearbyScannerConfig.kt   Feature entry point and configuration
├── wifi/        WifiScannerApi, ScannedWifiNetwork, WifiScanEvent, WifiScanConfig; WifiScannerImpl (internal)
├── bluetooth/   BluetoothScannerApi, ScannedBluetoothDevice, BluetoothScanEvent, BluetoothScanConfig;
│                BluetoothScannerImpl, BluetoothDeviceRegistry (internal)
└── ui/          NearbyScanScreen, navigation helpers; wifi/, bluetooth/, common/ (internal)
```

## API

### Navigation

| Declaration | Description |
|---|---|
| `NEARBY_SCAN_ROUTE` | Route of the feature screen: `"nearby_scan"`. |
| `NavGraphBuilder.nearbyScanScreen(scanner: NearbyScanner)` | Adds the feature screen to the host graph. |
| `NavController.navigateToNearbyScan(navOptions: NavOptions? = null)` | Opens the feature screen. |
| `@Composable NearbyScanScreen(scanner, modifier)` | The feature screen itself, with Wi-Fi and Bluetooth tabs. |

Scanning starts after the user presses Start and runs only while the screen is visible. Runtime permissions are requested by the screen.

### Scanners

| Declaration | Description |
|---|---|
| `NearbyScanner(context, config = NearbyScannerConfig())` | Creates both scanners. The primary constructor `NearbyScanner(wifi, bluetooth)` accepts fakes. |
| `WifiScannerApi(context, config)`, `BluetoothScannerApi(context, config)` | Create a single scanner if the app needs only one. |
| `requiredPermissions: Set<String>` | Runtime permissions the scanner needs. |
| `missingPermissions(): Set<String>` | Permissions that are not granted yet. |
| `scan(): Flow<…ScanEvent>` | Cold flow that scans while collected and completes by itself only after a terminal event. |
| `WifiScannerApi.isScanThrottlingEnabled: Boolean` | Android Wi-Fi scan throttling status from Developer options. |

### Output data

| Event | Meaning |
|---|---|
| `WifiScanEvent.Scanning`, `BluetoothScanEvent.Scanning` | The scan has started. |
| `WifiScanEvent.Networks(List<ScannedWifiNetwork>)` | Networks from the last scan: `ssid` (`null` for hidden), `bssid`, `rssiDbm`, `frequencyMhz`, `capabilities`. |
| `BluetoothScanEvent.Devices(List<ScannedBluetoothDevice>)` | Full device list, not a delta: `name` (`null` if not advertised), `address`, smoothed `rssiDbm`, `type`, `bondState`, `lastSeenAgoMillis`, `isStale`, raw `manufacturerData` (company ID → hex payload), `serviceUuids`. |

### Errors

All of the following are terminal: the flow completes after them.

| Event | Reasons / details |
|---|---|
| `PermissionRequired(permissions)` | Some of `requiredPermissions` are not granted. |
| `WifiScanEvent.Unavailable(reason)` | `WIFI_DISABLED`, `LOCATION_DISABLED`, `SCAN_REJECTED` (Android rejected the request and there are no cached results). |
| `BluetoothScanEvent.Unavailable(reason)` | `ADAPTER_UNAVAILABLE`, `BLUETOOTH_DISABLED`, `LE_SCANNER_UNAVAILABLE`. |
| `Error(message, cause)` | Unexpected failure. For Bluetooth the message contains the `ScanCallback` error code, e.g. `Bluetooth LE scan failed: 2`. |

### Configuration

| Parameter | Default | Meaning |
|---|---|---|
| `WifiScanConfig.throttledRefreshIntervalMillis` | 30 000 | Scan interval while Android scan throttling is enabled. |
| `WifiScanConfig.unthrottledRefreshIntervalMillis` | 5 000 | Scan interval while throttling is disabled. |
| `BluetoothScanConfig.snapshotIntervalMillis` | 750 | Devices are published at most this often. |
| `BluetoothScanConfig.ageRefreshIntervalMillis` | 5 000 | How often device ages are refreshed and expired devices removed. |
| `BluetoothScanConfig.staleAfterMillis` | 30 000 | A device not seen for this long gets `isStale = true`. |
| `BluetoothScanConfig.retentionMillis` | 120 000 | A device not seen for this long is removed. |
| `BluetoothScanConfig.rssiSmoothingFactor` | 0.25 | Exponential RSSI smoothing, `1` disables it. |
| `BluetoothScanConfig.minRssiChangeDbm` | 3 | Smaller RSSI changes are not published. |

### Permissions

The module manifest declares Wi-Fi state, location, `NEARBY_WIFI_DEVICES` (Android 13+), `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` (Android 12+), and legacy Bluetooth permissions (Android 11 and lower). They are merged into the host app.

## Usage examples

Add the module:

```kotlin
// build.gradle.kts of the host app
dependencies {
    implementation(project(":scanner"))
}
```

Create the scanner once, e.g. in `Application` or a DI container:

```kotlin
class App : Application() {
    val scanner: NearbyScanner by lazy { NearbyScanner(this) }
}
```

Register the screen in the host navigation graph and open it:

```kotlin
NavHost(navController = navController, startDestination = "home") {
    composable("home") {
        HomeScreen(onOpenScannerClick = { navController.navigateToNearbyScan() })
    }
    nearbyScanScreen(scanner = scanner)
}
```

Use a scanner without the feature UI:

```kotlin
val bluetooth = BluetoothScannerApi(context)

if (bluetooth.missingPermissions().isEmpty()) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            bluetooth.scan().collect { event ->
                when (event) {
                    is BluetoothScanEvent.Devices -> show(event.devices)
                    is BluetoothScanEvent.Unavailable -> showReason(event.reason)
                    else -> Unit
                }
            }
        }
    }
}
```
