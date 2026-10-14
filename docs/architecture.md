# Архитектура

## Цель

Вся логика мониторинга Wi-Fi и Bluetooth LE собрана в одном Android library-модуле `:monitoring`, чтобы его можно было подключить в другое приложение без остальных частей проекта. UI вынесен в отдельный модуль `:monitoring-ui` и нужен для проверки библиотеки; приложение-хост `:app` только собирает их вместе.

Radio (сотовая сеть) и Bluetooth Classic не поддерживаются: Radio не используется, а BLE-сканирование находит и LE-, и dual-mode устройства.

## Модули

```text
:app
  -> :monitoring-ui
       -> :monitoring (api)
  -> :monitoring
```

| Модуль | Тип | Содержимое |
|---|---|---|
| `:monitoring` | Android library, без Compose и DI | Публичный API, сканеры Wi-Fi/BLE, контроллер, foreground service, разрешения в манифесте, строки уведомления |
| `:monitoring-ui` | Android library, Compose | Экраны `WifiMonitoringScreen` и `BleMonitoringScreen`, их ViewModel, редьюсеры и мапперы |
| `:app` | Android application | Инициализация библиотеки и нижняя навигация по включённым источникам |

## Публичный API и внутренняя часть

Внутри `:monitoring` граница проведена через видимость Kotlin, а не через отдельные Gradle-модули:

```text
com.engboost.monitoringdevices.monitoring          — public: DeviceMonitoring, DeviceMonitoringConfig,
                                                     MonitoringSource, MonitoringState, StartResult
com.engboost.monitoringdevices.monitoring.wifi     — public: WifiScanEvent, WifiNetwork, WifiUnavailableReason
com.engboost.monitoringdevices.monitoring.ble      — public: BleScanEvent, BleDevice, BleDeviceType, ...
com.engboost.monitoringdevices.monitoring.internal — internal: контроллер, сервис, сканеры
```

Всё в `internal` недоступно потребителю библиотеки, поэтому реализацию можно менять без изменения API.

Библиотека не зависит от DI-фреймворка. Экземпляр создаётся через `DeviceMonitoring.initialize(context, config)` и берётся через `DeviceMonitoring.get(context)`; хост может положить его в Koin, Hilt или передать вручную. `DeviceMonitoring` — интерфейс, поэтому для UI-тестов его можно заменить фейком.

## Переключение источников

```kotlin
DeviceMonitoring.initialize(
    context = this,
    config = DeviceMonitoringConfig(
        enabledSources = setOf(MonitoringSource.WIFI, MonitoringSource.BLE)
    )
)
```

- `enabledSources` определяет, какие источники вообще доступны. `start()` для выключенного источника возвращает `StartResult.SourceDisabled`, тестовое приложение показывает вкладки только для включённых источников.
- Включённые источники запускаются и останавливаются независимо: `start(MonitoringSource.WIFI)`, `stop(MonitoringSource.BLE)`, `stopAll()`.
- Параметры каждого источника задаются в `WifiMonitoringConfig` и `BleMonitoringConfig`.

## Движение данных

```text
UI / потребитель
  start(source) ──────────────► DefaultDeviceMonitoring ── startForegroundService ──► DeviceMonitoringService
                                   │  state: StateFlow<MonitoringState>                   │
                                   │  последние события Wi-Fi / BLE                       │ scan job на источник
  wifiEvents() / bleEvents() ◄─────┘◄──────────── publish*Event ◄── AndroidWifiScanner / AndroidBleScanner
```

1. `start(source)` проверяет `enabledSources` и разрешения, добавляет источник в активные и запускает foreground service.
2. Сервис следит за `state.activeSources`, держит по одной scan job на источник и обновляет тип foreground service (`location` для Wi-Fi, `connectedDevice` для BLE).
3. Сканеры отдают события в контроллер. Последнее событие каждого источника хранится и повторяется новым подписчикам.
4. `wifiEvents()` / `bleEvents()` завершаются, когда источник остановлен или пришло терминальное событие (`PermissionRequired`, `Unavailable`, `Error`).
5. Активные источники сохраняются в `SharedPreferences`; если Android пересоздаёт процесс и сервис (`START_STICKY`), мониторинг продолжается.

## BLE: обработка результатов

`AndroidBleScanner` запускает нефильтрованный `BluetoothLeScanner.startScan`. Каждый advertising-пакет попадает в `BleDeviceRegistry` — класс без Android-зависимостей:

- RSSI сглаживается экспоненциально (`rssiSmoothingFactor`), изменения меньше `minRssiChangeDbm` не публикуются;
- snapshot отправляется не чаще `snapshotIntervalMillis` и только если что-то изменилось;
- раз в `ageRefreshIntervalMillis` обновляется возраст записей; устройство помечается `isStale` после `staleAfterMillis` и удаляется после `retentionMillis`.

Имя устройства берётся из `BluetoothDevice.name`, а если оно недоступно — из advertising-пакета (`ScanRecord.deviceName`).

## Wi-Fi: интервал сканирования

Сервис передаёт сканеру интервал как функцию, которая перечитывается перед каждым ожиданием: при включённом throttling используется `throttledRefreshIntervalMillis`, иначе `unthrottledRefreshIntervalMillis`. Если пользователь выключит throttling в Developer options во время работы, новый интервал применится со следующего цикла без перезапуска.

## Тесты

Логика без Android вынесена так, чтобы её можно было покрыть JVM unit-тестами:

- `monitoring/src/test` — `BleDeviceRegistryTest` (сглаживание, подавление мелких изменений, stale, удаление);
- `monitoring-ui/src/test` — `BleDeviceUiMapperTest` (режимы сортировки, стабильность порядка).

Запуск: `./gradlew :monitoring:testDebugUnitTest :monitoring-ui:testDebugUnitTest`.
