# `:monitoring`

Android library с полной логикой мониторинга Wi-Fi и Bluetooth LE. Зависит только от `androidx.core` и `kotlinx-coroutines`; Compose и DI-фреймворк не нужны.

## Публичный API

| Тип | Назначение |
|---|---|
| `DeviceMonitoring` | Точка входа: `initialize`/`get`, `start`/`stop`/`stopAll`, `state`, `wifiEvents()`, `bleEvents()`, разрешения |
| `DeviceMonitoringConfig` | `enabledSources` и параметры `WifiMonitoringConfig`, `BleMonitoringConfig`, `MonitoringNotificationConfig` |
| `MonitoringSource` | `WIFI`, `BLE` |
| `MonitoringState` | Активные источники и число найденных сетей и устройств |
| `StartResult` | `Started`, `PermissionsRequired`, `SourceDisabled`, `Failed` |
| `WifiScanEvent`, `WifiNetwork`, `WifiUnavailableReason` | События и модели Wi-Fi |
| `BleScanEvent`, `BleDevice`, `BleDeviceType`, `BleBondState`, `BleUnavailableReason` | События и модели BLE |

Причины недоступности — enum, тексты для пользователя формирует UI.

## Использование

```kotlin
val monitoring = DeviceMonitoring.get(context)

val missing = monitoring.missingPermissions(MonitoringSource.BLE)
// запросить missing через ActivityResultContracts.RequestMultiplePermissions

when (val result = monitoring.start(MonitoringSource.BLE)) {
    StartResult.Started -> scope.launch {
        monitoring.bleEvents().collect { event -> /* Scanning, Devices, ... */ }
    }
    is StartResult.PermissionsRequired -> requestPermissions(result.permissions)
    StartResult.SourceDisabled -> Unit
    is StartResult.Failed -> showError(result.cause)
}
```

## Внутреннее устройство

| Класс | Роль |
|---|---|
| `DeviceMonitoringProvider` | Процессный singleton, создаёт контроллер и сканеры |
| `DefaultDeviceMonitoring` | Реализация API: активные источники, проверка разрешений, хранение событий |
| `DeviceMonitoringService` | Foreground service, scan job на каждый активный источник, уведомление |
| `AndroidWifiScanner` | `WifiManager.startScan()`, `SCAN_RESULTS_AVAILABLE_ACTION`, cached results |
| `AndroidBleScanner` | `BluetoothLeScanner.startScan()` и передача результатов в `BleDeviceRegistry` |
| `BleDeviceRegistry` | Сглаживание RSSI, throttling snapshot, stale и удаление устройств; без Android-зависимостей |

## Манифест

Модуль объявляет разрешения обоих источников, foreground service-разрешения, `POST_NOTIFICATIONS` и сам сервис (`foregroundServiceType="connectedDevice|location"`). Если хост отключил источник, его разрешения можно убрать в манифесте приложения через `tools:node="remove"`.

## Ресурсы

Строки уведомления лежат в `res/values/strings.xml` с префиксом `device_monitoring_` и переопределяются в приложении. Иконку уведомления можно задать через `MonitoringNotificationConfig.smallIconRes`.
