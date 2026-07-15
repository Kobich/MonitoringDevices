# Архитектура приложения мониторинга устройств

## Цель

Приложение состоит из трех независимых пользовательских фич:

- Wi-Fi мониторинг
- Bluetooth мониторинг
- Radio мониторинг

Каждая фича оформлена отдельной парой Gradle-модулей `api/impl`. Низкоуровневая работа с Android API остается в `scanner:*` модулях.

## Модули

```text
:app
  Точка входа приложения и нижняя навигация между фичами.

:feature:wifi:api
  Публичный UI-контракт Wi-Fi фичи.

:feature:wifi:impl
  Экран, ViewModel, UI state и подключение Wi-Fi scanner.

:feature:bluetooth:api
  Публичный UI-контракт Bluetooth фичи.

:feature:bluetooth:impl
  Экран, ViewModel, UI state и подключение Bluetooth scanner.

:feature:radio:api
  Публичный UI-контракт Radio фичи.

:feature:radio:impl
  Экран, ViewModel, UI state и подключение Radio scanner.

:scanner:wifi:api / :scanner:wifi:impl
  Низкоуровневый Wi-Fi scanner.

:scanner:bluetooth:api / :scanner:bluetooth:impl
  Низкоуровневый Bluetooth scanner.

:scanner:radio:api / :scanner:radio:impl
  Низкоуровневый Radio scanner.
```

## Зависимости

```text
app
  -> feature:wifi:impl
  -> feature:bluetooth:impl
  -> feature:radio:impl

feature:wifi:impl
  -> feature:wifi:api
  -> scanner:wifi:impl

feature:bluetooth:impl
  -> feature:bluetooth:api
  -> scanner:bluetooth:impl

feature:radio:impl
  -> feature:radio:api
  -> scanner:radio:impl

scanner:*:impl
  -> scanner:*:api
```

## UI и UDF

Внутри каждой feature `impl` используется схема:

```text
Route -> Screen(state, callbacks)
ViewModel -> UiState -> Screen
Screen -> callback -> ViewModel
```

Правила:

- `Screen` не получает `ViewModel`.
- `Screen` не создает state.
- `UiState` immutable и помечен `@Immutable`.
- Навигация между Wi-Fi/Bluetooth/Radio находится в `app`, потому что это сборка верхнего уровня, а не отдельная бизнес-фича.

## Границы ответственности

- `app` только собирает фичи и переключает табы.
- `feature:*:api` содержит только внешний UI-контракт.
- `feature:*:impl` владеет своим экраном и состоянием.
- `scanner:*` владеет Android framework API и не знает про UI-фичи.

## Scanner API

Каждый scanner-модуль отдает данные через `Flow`. Сбор данных начинается при collection и останавливается при отмене collection.

```text
scanner:wifi:api
  WifiScanner.scan(WifiScanConfig): Flow<WifiScanEvent>
  modes: ACTIVE, CACHED_ONLY; optional refresh interval; scan throttling status
  data: SSID, BSSID, RSSI, frequency, capabilities

scanner:bluetooth:api
  BluetoothScanner.scan(BluetoothScanConfig): Flow<BluetoothScanEvent>
  modes: BLE, CLASSIC, ALL
  data: name, address, RSSI, type, bond state

scanner:radio:api
  RadioScanner.scan(RadioScanConfig): Flow<RadioScanEvent>
  modes: REGISTERED_ONLY, ALL_AVAILABLE
  data: network type, registration state, signal strength, operator
```

Scanner events have the same shape:

- `Scanning` — scanner started.
- `PermissionRequired` — caller must request runtime permissions.
- `Unavailable` — required device capability or platform setting is missing.
- `Error` — platform API failed unexpectedly.
- Result event — `Networks`, `Devices` or `Cells`.
