# MonitoringDevices

Android-библиотека сканирования ближайших Wi-Fi сетей и Bluetooth-устройств (`:scanner`), тестовые экраны к ней (`:scanner-ui`) и приложение-хост (`:app`).

```kotlin
val scanner = NearbyScanner(applicationContext)
scanner.wifi.scan().collect { event -> /* ... */ }
```

Документация: [docs/README.md](docs/README.md).
