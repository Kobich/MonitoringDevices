# `:monitoring`

Android library со всей логикой сканирования Wi-Fi и Bluetooth. Зависит только от `kotlinx-coroutines`; Compose, androidx и DI-фреймворк не нужны. Фоновой работы нет: сканирование идёт, пока собирается Flow.

Раскладка пакетов описана в [архитектуре](../architecture.md#как-ориентироваться-в-monitoring).

## Публичный API

| Тип | Пакет | Назначение |
|---|---|---|
| `DeviceMonitoring`, `DeviceMonitoring(context, config)` | корень | Точка входа: `scanWifi()`, `scanBluetooth()`, разрешения, статус Wi-Fi throttling |
| `DeviceMonitoringConfig` | корень | `enabledSources` и конфиги источников |
| `MonitoringSource` | корень | `WIFI`, `BLUETOOTH` |
| `WifiNetwork`, `WifiScanEvent`, `WifiUnavailableReason`, `WifiMonitoringConfig` | `wifi` | Модели, события и параметры Wi-Fi |
| `BluetoothDeviceInfo`, `BluetoothScanEvent`, `BluetoothUnavailableReason`, `BluetoothMonitoringConfig` | `bluetooth` | Модели, события и параметры Bluetooth |

Причины недоступности — enum, тексты для пользователя формирует UI.

## Использование

```kotlin
val monitoring = DeviceMonitoring(applicationContext)

val missing = monitoring.missingPermissions(MonitoringSource.BLUETOOTH)
if (missing.isNotEmpty()) {
    requestPermissions(missing)
} else {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            monitoring.scanBluetooth().collect { event -> /* Scanning, Devices, ... */ }
        }
    }
}
```

## Манифест

Модуль объявляет только разрешения сканирования обоих источников. Если хост отключил источник, его разрешения можно убрать в манифесте приложения через `tools:node="remove"`.
