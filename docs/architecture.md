# Архитектура

## Цель

Вся логика мониторинга Wi-Fi и Bluetooth собрана в одном Android library-модуле `:monitoring`, чтобы его можно было подключить в другое приложение без остальных частей проекта. UI вынесен в отдельный модуль `:monitoring-ui` и нужен для проверки библиотеки; приложение-хост `:app` только собирает их вместе.

Radio (сотовая сеть) и Bluetooth Classic не поддерживаются. Bluetooth сканируется через Bluetooth LE: такой скан находит и LE-, и dual-mode устройства.

## Модули

```text
:app
  -> :monitoring-ui
       -> :monitoring (api)
  -> :monitoring
```

| Модуль | Тип | Содержимое |
|---|---|---|
| `:monitoring` | Android library, без Compose и DI | Публичный API, сканеры Wi-Fi и Bluetooth, foreground service, разрешения в манифесте, строки уведомления |
| `:monitoring-ui` | Android library, Compose | Экраны `WifiMonitoringScreen` и `BluetoothMonitoringScreen` с ViewModel |
| `:app` | Android application | Инициализация библиотеки и нижняя навигация по включённым источникам |

## Как ориентироваться в `:monitoring`

Код разложен по источникам. Всё про Wi-Fi лежит в `wifi/`, всё про Bluetooth — в `bluetooth/`; в корне только точка входа, а в `service/` — foreground service. Что доступно потребителю библиотеки, определяет модификатор `internal`, а не папка: публичные классы и `internal`-реализация лежат рядом.

```text
com.engboost.monitoringdevices.monitoring
├── DeviceMonitoring.kt            public    Точка входа: start/stop, state, события, MonitoringSource, StartResult
├── DeviceMonitoringConfig.kt      public    Корневая конфигурация: enabledSources, wifi, bluetooth, notification
├── DefaultDeviceMonitoring.kt     internal  Реализация DeviceMonitoring и единственный экземпляр на процесс
├── wifi/
│   ├── WifiNetwork.kt             public    Найденная сеть
│   ├── WifiScanEvent.kt           public    События сессии и WifiUnavailableReason
│   ├── WifiMonitoringConfig.kt    public    Интервалы сканирования
│   ├── WifiScanner.kt             internal  Запрос сканов и чтение результатов
│   └── WifiScanAvailability.kt    internal  Разрешения, Wi-Fi/Location, scan throttling
├── bluetooth/
│   ├── BluetoothDeviceInfo.kt     public    Найденное устройство, тип и состояние сопряжения
│   ├── BluetoothScanEvent.kt      public    События сессии и BluetoothUnavailableReason
│   ├── BluetoothMonitoringConfig.kt public  Сглаживание RSSI, stale, удаление устройств
│   ├── BluetoothScanner.kt        internal  Bluetooth LE scan
│   ├── BluetoothScanAvailability.kt internal Разрешения и состояние адаптера
│   └── BluetoothDeviceRegistry.kt internal  Накопление пакетов между snapshot, без Android-зависимостей
└── service/
    ├── DeviceMonitoringService.kt internal  Foreground service, по одной scan job на источник
    └── MonitoringNotification.kt  internal  Канал и содержимое уведомления
```

Правило именования: в каждом пакете источника одинаковый набор файлов — модель, `*ScanEvent`, `*MonitoringConfig`, `*Scanner`, `*ScanAvailability`.

## Переключение источников

```kotlin
DeviceMonitoring.initialize(
    context = this,
    config = DeviceMonitoringConfig(
        enabledSources = setOf(MonitoringSource.WIFI, MonitoringSource.BLUETOOTH)
    )
)
```

- `enabledSources` определяет, какие источники вообще доступны. `start()` для выключенного источника возвращает `StartResult.SourceDisabled`, тестовое приложение показывает вкладки только для включённых источников.
- Включённые источники запускаются и останавливаются независимо: `start(MonitoringSource.WIFI)`, `stop(MonitoringSource.BLUETOOTH)`, `stopAll()`.
- Параметры источников задаются в `WifiMonitoringConfig` и `BluetoothMonitoringConfig`.

Библиотека не зависит от DI-фреймворка. Экземпляр создаётся через `DeviceMonitoring.initialize(context, config)` и берётся через `DeviceMonitoring.get(context)`; хост может положить его в Koin, Hilt или передать вручную. `DeviceMonitoring` — интерфейс, поэтому для UI-тестов его можно заменить фейком.

## Движение данных

```text
UI / потребитель
  start(source) ─────────────────► DefaultDeviceMonitoring ── startForegroundService ──► DeviceMonitoringService
                                     │  state: StateFlow<MonitoringState>                    │
                                     │  последнее событие каждого источника                  │ scan job на источник
  wifiEvents() / bluetoothEvents() ◄─┘◄───────────── publish*Event ◄──── WifiScanner / BluetoothScanner
```

1. `start(source)` проверяет `enabledSources` и разрешения, добавляет источник в активные и запускает foreground service.
2. Сервис следит за `state.activeSources`, держит по одной scan job на источник и обновляет тип foreground service (`location` для Wi-Fi, `connectedDevice` для Bluetooth).
3. Сканеры отдают события в `DefaultDeviceMonitoring`. Последнее событие каждого источника хранится и повторяется новым подписчикам.
4. `wifiEvents()` / `bluetoothEvents()` завершаются, когда источник остановлен или пришло терминальное событие (`PermissionRequired`, `Unavailable`, `Error`).
5. Активные источники сохраняются в `SharedPreferences`; если Android пересоздаёт процесс и сервис (`START_STICKY`), мониторинг продолжается.

## Bluetooth: обработка результатов

`BluetoothScanner` запускает нефильтрованный `BluetoothLeScanner.startScan`. Каждый advertising-пакет попадает в `BluetoothDeviceRegistry`:

- RSSI сглаживается экспоненциально (`rssiSmoothingFactor`), изменения меньше `minRssiChangeDbm` не публикуются;
- snapshot отправляется не чаще `snapshotIntervalMillis` и только если что-то изменилось;
- раз в `ageRefreshIntervalMillis` обновляется возраст записей; устройство помечается `isStale` после `staleAfterMillis` и удаляется после `retentionMillis`.

Имя устройства берётся из `BluetoothDevice.name`, а если оно недоступно — из advertising-пакета (`ScanRecord.deviceName`).

## Wi-Fi: интервал сканирования

Сервис передаёт сканеру интервал как функцию, которая вызывается перед каждым ожиданием: при включённом throttling используется `throttledRefreshIntervalMillis`, иначе `unthrottledRefreshIntervalMillis`. Если пользователь выключит throttling в Developer options во время работы, новый интервал применится со следующего цикла без перезапуска.

## `:monitoring-ui`

Устроен так же: пакеты `wifi/` и `bluetooth/` с одинаковым набором файлов и `common/` для общего.

```text
com.engboost.monitoringdevices.monitoring.ui
├── common/      PermissionRequestUi, PermissionRequestEffect, StatusCard, DetailRow, StartStopButtons, тексты ошибок
├── wifi/        WifiMonitoringScreen (public) → WifiViewModel → WifiUiState + WifiUiStateReducer → WifiScreen
└── bluetooth/   BluetoothMonitoringScreen (public) → BluetoothViewModel → BluetoothUiState + BluetoothUiStateReducer → BluetoothScreen
```

- `*MonitoringScreen` — публичная точка входа: создаёт ViewModel и запрашивает разрешения.
- `*ViewModel` — вызывает `DeviceMonitoring` и собирает события.
- `*UiStateReducer` — чистые функции «состояние + событие → состояние».
- `*Screen`, `*Card`, `*DetailsSheet` — только отрисовка.

## Тесты

Логика без Android покрыта JVM unit-тестами:

- `monitoring/src/test` — `BluetoothDeviceRegistryTest` (сглаживание, подавление мелких изменений, stale, удаление);
- `monitoring-ui/src/test` — `BluetoothDeviceUiMapperTest` (режимы сортировки, стабильность порядка).

Запуск: `./gradlew :monitoring:testDebugUnitTest :monitoring-ui:testDebugUnitTest`.

## Комментарии в коде

Комментарии следуют [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html#documentation-comments): KDoc (`/** */`) у публичного API и неочевидных internal-классов, первое предложение — краткое описание, параметры упоминаются в тексте через `[name]`, свойства классов данных описываются через `@property`. Однострочные `//` объясняют только неочевидные решения.
