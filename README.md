# MonitoringDevices

Android project with the nearby scanner feature and a sample host app.

| Module | Description |
|---|---|
| [`:scanner`](scanner/README.md) | Feature: Wi-Fi and Bluetooth scanners and their Compose screens. |
| `:app` | Sample host app: a home screen that opens the feature through Compose Navigation. |

Opening the feature from a host app:

```kotlin
NavHost(navController = navController, startDestination = "home") {
    composable("home") { HomeScreen(onOpenScannerClick = { navController.navigateToNearbyScan() }) }
    nearbyScanScreen(scanner = NearbyScanner(applicationContext))
}
```

Build and run the tests:

```bash
./gradlew :app:assembleDebug :scanner:testDebugUnitTest
```

Detailed design notes (in Russian) are in [docs/](docs/README.md).
