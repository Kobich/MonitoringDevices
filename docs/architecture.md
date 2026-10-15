# Архитектура

## Цель

Вся логика сканирования Wi-Fi и Bluetooth собрана в одном Android library-модуле `:scanner`, чтобы его можно было подключить в другое приложение без остальных частей проекта. UI вынесен в отдельный модуль `:scanner-ui` и нужен для проверки библиотеки; приложение-хост `:app` только собирает их вместе.

Сценарий использования — открыли приложение, посмотрели окружение, закрыли. Фоновой работы нет: сканирование идёт, пока экран открыт.

Radio (сотовая сеть) и Bluetooth Classic не поддерживаются. Bluetooth сканируется через Bluetooth LE: такой скан находит и LE-, и dual-mode устройства.

## Модули

```text
:app
  -> :scanner-ui
       -> :scanner (api)
  -> :scanner
```

| Модуль | Тип | Содержимое |
|---|---|---|
| `:scanner` | Android library, зависит только от `kotlinx-coroutines` | API и реализация сканеров Wi-Fi и Bluetooth, разрешения в манифесте |
| `:scanner-ui` | Android library, Compose | Экраны `WifiScanScreen` и `BluetoothScanScreen` с ViewModel |
| `:app` | Android application | Создание `NearbyScanner` и нижняя навигация по вкладкам |

## Как ориентироваться в `:scanner`

Каждый сканер — самостоятельный пакет: публичный интерфейс `*ScannerApi`, его модели, события и конфигурация лежат рядом, а реализация — `internal`-класс `*ScannerImpl`. В корне только `NearbyScanner`, который объединяет оба сканера.

```text
com.engboost.monitoringdevices.scanner
├── NearbyScanner.kt                 public    API фичи: wifi + bluetooth
├── NearbyScannerConfig.kt           public    Конфигурация обоих сканеров
├── wifi/
│   ├── WifiScannerApi.kt            public    API сканера и фабрика WifiScannerApi(context, config)
│   ├── ScannedWifiNetwork.kt        public    Найденная сеть
│   ├── WifiScanEvent.kt             public    События скана и WifiUnavailableReason
│   ├── WifiScanConfig.kt            public    Интервалы сканирования
│   ├── WifiScannerImpl.kt           internal  Реализация на WifiManager
│   └── WifiScanAvailability.kt      internal  Разрешения, scan throttling, состояние Wi-Fi и Location
└── bluetooth/
    ├── BluetoothScannerApi.kt       public    API сканера и фабрика BluetoothScannerApi(context, config)
    ├── ScannedBluetoothDevice.kt    public    Найденное устройство, тип и состояние сопряжения
    ├── BluetoothScanEvent.kt        public    События скана и BluetoothUnavailableReason
    ├── BluetoothScanConfig.kt       public    Сглаживание RSSI, stale, удаление устройств
    ├── BluetoothScannerImpl.kt      internal  Реализация на BluetoothLeScanner
    ├── BluetoothScanAvailability.kt internal  Разрешения и состояние адаптера
    └── BluetoothDeviceRegistry.kt   internal  Накопление пакетов между snapshot, без Android-зависимостей
```

Правило именования: `<Source>ScannerApi` — API, `<Source>ScannerImpl` — реализация, `Scanned<Entity>` — найденная сущность, `<Source>ScanEvent`, `<Source>ScanConfig`, `<Source>ScanAvailability` — что нужно для запуска скана.

## Использование и переключение

```kotlin
val scanner = NearbyScanner(applicationContext)

scanner.wifi.scan()       // Flow<WifiScanEvent>
scanner.bluetooth.scan()  // Flow<BluetoothScanEvent>
```

- Сканеры независимы. Если приложению нужен только один, оно использует только его: `WifiScannerApi(context)` или `BluetoothScannerApi(context)`.
- В тестовом приложении вкладки перечислены в `enabledTabs` в `MainActivity`.
- Библиотека не зависит от DI-фреймворка и не держит singleton. Хост создаёт экземпляры и хранит их в `Application`, Koin, Hilt или передаёт вручную. Для тестов в `NearbyScanner` передаются фейковые реализации `WifiScannerApi` и `BluetoothScannerApi`.

## Движение данных

```text
Экран (STARTED) ── collect ──► WifiScannerApi.scan()      ──► WifiManager
                ── collect ──► BluetoothScannerApi.scan() ──► BluetoothLeScanner
```

1. Перед сканом UI проверяет `missingPermissions()` и запрашивает недостающие разрешения.
2. `scan()` возвращает холодный Flow: Android-скан запускается при начале сбора и останавливается при отмене.
3. Flow завершается сам только после терминального события (`PermissionRequired`, `Unavailable`, `Error`).

## Bluetooth: обработка результатов

`BluetoothScannerImpl` запускает нефильтрованный `BluetoothLeScanner.startScan`. Каждый advertising-пакет попадает в `BluetoothDeviceRegistry`:

- RSSI сглаживается экспоненциально (`rssiSmoothingFactor`), изменения меньше `minRssiChangeDbm` не публикуются;
- snapshot отправляется не чаще `snapshotIntervalMillis` и только если что-то изменилось;
- раз в `ageRefreshIntervalMillis` обновляется возраст записей; устройство помечается `isStale` после `staleAfterMillis` и удаляется после `retentionMillis`.

Имя устройства берётся из `BluetoothDevice.name`, а если оно недоступно — из advertising-пакета (`ScanRecord.deviceName`). Устройства без имени, например датчики, которые транслируют только MAC-адрес, остаются в списке с `name = null`.

Из пакета также передаются сырые данные: `manufacturerData` (идентификатор компании от Bluetooth SIG → payload в hex) и `serviceUuids`. Формат payload у каждого производителя свой, поэтому он не расшифровывается.

## Wi-Fi: интервал сканирования

Перед каждым ожиданием `WifiScannerImpl` перечитывает `isScanThrottlingEnabled`: при включённом throttling используется `throttledRefreshIntervalMillis`, иначе `unthrottledRefreshIntervalMillis`. Если пользователь выключит throttling в Developer options во время работы, новый интервал применится со следующего цикла.

## `:scanner-ui`

Устроен так же: пакеты `wifi/` и `bluetooth/` с одинаковым набором файлов и `common/` для общего.

```text
com.engboost.monitoringdevices.scanner.ui
├── common/      ScanStatus, PermissionRequestUi, PermissionRequestEffect, ScanWhileVisibleEffect,
│                StatusCard, DetailRow, StartStopButtons, тексты ошибок
├── wifi/        WifiScanScreen (public) → WifiViewModel → WifiUiState + WifiUiStateReducer → WifiScanContent
└── bluetooth/   BluetoothScanScreen (public) → BluetoothViewModel → BluetoothUiState + BluetoothUiStateReducer
                 → BluetoothScanContent
```

- `*ScanScreen` — публичная точка входа: создаёт ViewModel, запрашивает разрешения и запускает скан, пока экран виден.
- `*ViewModel` — держит `StateFlow` состояния. Единственный источник правды — `ScanStatus`: скан идёт, пока статус равен `Scanning`. Start и Stop меняют статус, терминальное событие заменяет его и тем самым отменяет скан.
- `*UiStateReducer` — чистые функции «состояние + событие → состояние».
- `*ScanContent`, `*Card`, `*DetailsSheet` — только отрисовка. Тексты статусов и причин формируются здесь.

## Тесты

Логика без Android покрыта JVM unit-тестами:

- `scanner/src/test` — `BluetoothDeviceRegistryTest` (сглаживание, подавление мелких изменений, stale, удаление);
- `scanner-ui/src/test` — `BluetoothDeviceUiMapperTest` (режимы сортировки, стабильность порядка), `WifiUiStateReducerTest` (статусы, список сетей, запрос разрешений).

Запуск: `./gradlew :scanner:testDebugUnitTest :scanner-ui:testDebugUnitTest`.

## Комментарии в коде

Комментарии в коде пишутся только на английском и следуют [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html#documentation-comments): KDoc (`/** */`) обязателен у публичного API, у `internal` — только если поведение не очевидно из имени. Первое предложение — краткое описание, параметры упоминаются в тексте через `[name]`, свойства классов данных описываются через `@property`. Однострочные `//` объясняют только неочевидные решения.
