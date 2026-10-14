# `:monitoring`

Android library с полной логикой мониторинга Wi-Fi и Bluetooth. Зависит только от `androidx.core` и `kotlinx-coroutines`; Compose и DI-фреймворк не нужны.

Раскладка пакетов описана в [архитектуре](../architecture.md#как-ориентироваться-в-monitoring).

## Публичный API

| Тип | Пакет | Назначение |
|---|---|---|
| `DeviceMonitoring` | корень | Точка входа: `initialize`/`get`, `start`/`stop`/`stopAll`, `state`, `wifiEvents()`, `bluetoothEvents()`, разрешения |
| `DeviceMonitoringConfig`, `MonitoringNotificationConfig` | корень | `enabledSources`, конфиги источников, иконка уведомления |
| `MonitoringSource`, `MonitoringState`, `StartResult` | корень | Источники, текущее состояние, результат запуска |
| `WifiNetwork`, `WifiScanEvent`, `WifiUnavailableReason`, `WifiMonitoringConfig` | `wifi` | Модели, события и параметры Wi-Fi |
| `BluetoothDeviceInfo`, `BluetoothScanEvent`, `BluetoothUnavailableReason`, `BluetoothMonitoringConfig` | `bluetooth` | Модели, события и параметры Bluetooth |

Причины недоступности — enum, тексты для пользователя формирует UI.

## Использование

```kotlin
val monitoring = DeviceMonitoring.get(context)

when (val result = monitoring.start(MonitoringSource.BLUETOOTH)) {
    StartResult.Started -> scope.launch {
        monitoring.bluetoothEvents().collect { event -> /* Scanning, Devices, ... */ }
    }
    is StartResult.PermissionsRequired -> requestPermissions(result.permissions)
    StartResult.SourceDisabled -> Unit
    is StartResult.Failed -> showError(result.cause)
}
```

## Манифест

Модуль объявляет разрешения обоих источников, foreground service-разрешения, `POST_NOTIFICATIONS` и сам сервис (`foregroundServiceType="connectedDevice|location"`). Если хост отключил источник, его разрешения можно убрать в манифесте приложения через `tools:node="remove"`.

## Ресурсы

Строки уведомления лежат в `res/values/strings.xml` с префиксом `device_monitoring_` и переопределяются в приложении. Иконку уведомления можно задать через `MonitoringNotificationConfig.smallIconRes`.
