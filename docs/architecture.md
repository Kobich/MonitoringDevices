# Архитектура

## Цель

Вся логика сканирования Wi-Fi и Bluetooth собрана в одном Android library-модуле `:monitoring`, чтобы его можно было подключить в другое приложение без остальных частей проекта. UI вынесен в отдельный модуль `:monitoring-ui` и нужен для проверки библиотеки; приложение-хост `:app` только собирает их вместе.

Сценарий использования — открыли приложение, посмотрели окружение, закрыли. Фоновой работы нет: сканирование идёт, пока экран открыт.

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
| `:monitoring` | Android library, зависит только от `kotlinx-coroutines` | Публичный API, сканеры Wi-Fi и Bluetooth, разрешения в манифесте |
| `:monitoring-ui` | Android library, Compose | Экраны `WifiMonitoringScreen` и `BluetoothMonitoringScreen` с ViewModel |
| `:app` | Android application | Инициализация библиотеки и нижняя навигация по включённым источникам |

## Как ориентироваться в `:monitoring`

Код разложен по источникам. Всё про Wi-Fi лежит в `wifi/`, всё про Bluetooth — в `bluetooth/`; в корне только точка входа. Что доступно потребителю библиотеки, определяет модификатор `internal`, а не папка: публичные классы и `internal`-реализация лежат рядом.

```text
com.engboost.monitoringdevices.monitoring
├── DeviceMonitoring.kt              public    Точка входа: scanWifi/scanBluetooth, разрешения, MonitoringSource
├── DeviceMonitoringConfig.kt        public    Корневая конфигурация: enabledSources, wifi, bluetooth
├── DefaultDeviceMonitoring.kt       internal  Реализация DeviceMonitoring
├── wifi/
│   ├── WifiNetwork.kt               public    Найденная сеть
│   ├── WifiScanEvent.kt             public    События скана и WifiUnavailableReason
│   ├── WifiMonitoringConfig.kt      public    Интервалы сканирования
│   ├── WifiScanner.kt               internal  Запрос сканов и чтение результатов
│   └── WifiScanAvailability.kt      internal  Разрешения, Wi-Fi/Location, scan throttling
└── bluetooth/
    ├── BluetoothDeviceInfo.kt       public    Найденное устройство, тип и состояние сопряжения
    ├── BluetoothScanEvent.kt        public    События скана и BluetoothUnavailableReason
    ├── BluetoothMonitoringConfig.kt public    Сглаживание RSSI, stale, удаление устройств
    ├── BluetoothScanner.kt          internal  Bluetooth LE scan
    ├── BluetoothScanAvailability.kt internal  Разрешения и состояние адаптера
    └── BluetoothDeviceRegistry.kt   internal  Накопление пакетов между snapshot, без Android-зависимостей
```

Правило именования: в каждом пакете источника одинаковый набор файлов — модель, `*ScanEvent`, `*MonitoringConfig`, `*Scanner`, `*ScanAvailability`.

## Переключение источников

```kotlin
val monitoring = DeviceMonitoring(
    context = applicationContext,
    config = DeviceMonitoringConfig(
        enabledSources = setOf(MonitoringSource.WIFI, MonitoringSource.BLUETOOTH)
    )
)
```

- `enabledSources` определяет, какие источники доступны. Скан выключенного источника сразу отдаёт `Unavailable(SOURCE_DISABLED)`, тестовое приложение показывает вкладки только для включённых источников.
- Источники независимы: каждый скан — отдельный Flow.
- Параметры источников задаются в `WifiMonitoringConfig` и `BluetoothMonitoringConfig`.

Библиотека не зависит от DI-фреймворка и не держит singleton. Хост создаёт экземпляр через `DeviceMonitoring(context, config)` и хранит его в `Application`, Koin, Hilt или передаёт вручную. `DeviceMonitoring` — интерфейс, поэтому для UI-тестов его можно заменить фейком.

## Движение данных

```text
Экран (STARTED) ── collect ──► DeviceMonitoring.scanWifi()      ──► WifiScanner      ──► WifiManager
                ── collect ──► DeviceMonitoring.scanBluetooth() ──► BluetoothScanner ──► BluetoothLeScanner
```

1. Перед сканом UI проверяет `missingPermissions(source)` и запрашивает недостающие разрешения.
2. `scanWifi()` / `scanBluetooth()` возвращают холодный Flow: Android-скан запускается при начале сбора и останавливается при отмене.
3. Flow завершается сам только после терминального события (`PermissionRequired`, `Unavailable`, `Error`).
4. В тестовом UI ViewModel хранит, нажал ли пользователь Start, а экран собирает Flow через `repeatOnLifecycle(STARTED)`. При сворачивании приложения скан останавливается, при возврате — продолжается.

## Bluetooth: обработка результатов

`BluetoothScanner` запускает нефильтрованный `BluetoothLeScanner.startScan`. Каждый advertising-пакет попадает в `BluetoothDeviceRegistry`:

- RSSI сглаживается экспоненциально (`rssiSmoothingFactor`), изменения меньше `minRssiChangeDbm` не публикуются;
- snapshot отправляется не чаще `snapshotIntervalMillis` и только если что-то изменилось;
- раз в `ageRefreshIntervalMillis` обновляется возраст записей; устройство помечается `isStale` после `staleAfterMillis` и удаляется после `retentionMillis`.

Имя устройства берётся из `BluetoothDevice.name`, а если оно недоступно — из advertising-пакета (`ScanRecord.deviceName`).

## Wi-Fi: интервал сканирования

`DefaultDeviceMonitoring` передаёт сканеру интервал как функцию, которая вызывается перед каждым ожиданием: при включённом throttling используется `throttledRefreshIntervalMillis`, иначе `unthrottledRefreshIntervalMillis`. Если пользователь выключит throttling в Developer options во время работы, новый интервал применится со следующего цикла без перезапуска.

## `:monitoring-ui`

Устроен так же: пакеты `wifi/` и `bluetooth/` с одинаковым набором файлов и `common/` для общего.

```text
com.engboost.monitoringdevices.monitoring.ui
├── common/      PermissionRequestUi, PermissionRequestEffect, ScanWhileVisibleEffect, StatusCard, DetailRow, StartStopButtons, тексты ошибок
├── wifi/        WifiMonitoringScreen (public) → WifiViewModel → WifiUiState + WifiUiStateReducer → WifiScreen
└── bluetooth/   BluetoothMonitoringScreen (public) → BluetoothViewModel → BluetoothUiState + BluetoothUiStateReducer → BluetoothScreen
```

- `*MonitoringScreen` — публичная точка входа: создаёт ViewModel, запрашивает разрешения и запускает скан, пока экран виден.
- `*ViewModel` — хранит, запущен ли скан пользователем, и собирает события `DeviceMonitoring`.
- `*UiStateReducer` — чистые функции «состояние + событие → состояние».
- `*Screen`, `*Card`, `*DetailsSheet` — только отрисовка.

## Тесты

Логика без Android покрыта JVM unit-тестами:

- `monitoring/src/test` — `BluetoothDeviceRegistryTest` (сглаживание, подавление мелких изменений, stale, удаление);
- `monitoring-ui/src/test` — `BluetoothDeviceUiMapperTest` (режимы сортировки, стабильность порядка).

Запуск: `./gradlew :monitoring:testDebugUnitTest :monitoring-ui:testDebugUnitTest`.

## Комментарии в коде

Комментарии следуют [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html#documentation-comments): KDoc (`/** */`) у публичного API и неочевидных internal-классов, первое предложение — краткое описание, параметры упоминаются в тексте через `[name]`, свойства классов данных описываются через `@property`. Однострочные `//` объясняют только неочевидные решения.
