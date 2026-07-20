# Архитектура приложения мониторинга устройств

## Цель

Приложение состоит из трёх независимых пользовательских фич:

- Wi-Fi мониторинг;
- Bluetooth мониторинг;
- Radio мониторинг.

Каждая фича оформлена отдельной парой Gradle-модулей `api/impl`. Низкоуровневая работа с Android API находится в `scanner:*`, а продолжительное Wi-Fi/Bluetooth-сканирование координирует тонкий foreground service-слой `service:monitoring`.

## Модули

```text
:app
  Точка входа, DI-сборка и нижняя навигация между фичами.

:feature:wifi:api / :feature:wifi:impl
  UI-контракт, экран, ViewModel и представление Wi-Fi данных.

:feature:bluetooth:api / :feature:bluetooth:impl
  UI-контракт, экран, ViewModel и представление Bluetooth данных.

:feature:radio:api / :feature:radio:impl
  UI-контракт, экран, ViewModel и представление Radio данных.

:service:monitoring:api
  Контракт запуска и остановки Wi-Fi/Bluetooth мониторинга,
  состояние активных сканов и потоки их событий.

:service:monitoring:impl
  Один foreground service, две независимые scan job,
  уведомление и восстановление активных типов после пересоздания процесса.

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
  -> service:monitoring:impl
  -> scanner:wifi:impl
  -> scanner:bluetooth:impl
  -> scanner:radio:impl

feature:wifi:impl
  -> feature:wifi:api
  -> scanner:wifi:api
  -> service:monitoring:api

feature:bluetooth:impl
  -> feature:bluetooth:api
  -> scanner:bluetooth:api
  -> service:monitoring:api

feature:radio:impl
  -> feature:radio:api
  -> scanner:radio:api

service:monitoring:api
  -> scanner:wifi:api
  -> scanner:bluetooth:api

service:monitoring:impl
  -> service:monitoring:api

scanner:*:impl
  -> scanner:*:api
```

`feature:wifi` и `feature:bluetooth` остаются владельцами пользовательского поведения и отображения данных. `service:monitoring` не является объединённой фичей и не содержит экранов: это только процессная прослойка над scanner-модулями.

## Foreground Monitoring

Запуск выполняется по требованию фичи:

1. `WifiScanInteractor` или `BluetoothScanInteractor` вызывает `MonitoringController.startMonitoring(type)`.
2. Controller проверяет runtime permissions, сохраняет активный тип и запускает `DeviceMonitoringService`.
3. Service поднимается в foreground и создаёт отдельную coroutine job для каждого активного типа.
4. События scanner-а публикуются через controller обратно в соответствующую фичу.
5. Явный Stop останавливает только выбранный тип. Service завершается, когда активных типов не осталось.

Wi-Fi и Bluetooth работают независимо внутри одного service:

```text
active = WIFI                -> Wi-Fi job
active = BLUETOOTH           -> Bluetooth job
active = WIFI + BLUETOOTH    -> Wi-Fi job + Bluetooth job
active = empty               -> service stops
```

Переход на другую вкладку или уничтожение ViewModel не останавливает активное сканирование. При возврате экран подключается к текущему потоку. Явная кнопка Stop или действие `Stop all` в уведомлении меняет состояние мониторинга.

Активные типы сохраняются в `SharedPreferences`. `START_STICKY` позволяет Android пересоздать service после уничтожения процесса; после пересоздания запускаются только ранее активные scan job. Это не гарантирует непрерывность при принудительной остановке приложения пользователем или ограничениях производителя устройства.

Тип foreground service формируется из активных сканов:

- Wi-Fi использует `location`;
- Bluetooth использует `connectedDevice`;
- при совместной работе используются оба типа.

## UI и UDF

Внутри каждого feature `impl` используется схема:

```text
Route -> Screen(state, callbacks)
ViewModel -> UiState -> Screen
Screen -> callback -> ViewModel
```

Правила:

- `Screen` не получает `ViewModel` и не создаёт state;
- `UiState` immutable и помечен `@Immutable`;
- навигация между Wi-Fi/Bluetooth/Radio находится в `app`;
- ViewModel подписывается на события monitoring-слоя, но не владеет lifecycle scan job;
- отмена UI collection не равна остановке мониторинга.

## Границы ответственности

- `app` собирает DI-граф, фичи и навигацию;
- `feature:*:api` содержит внешний UI-контракт;
- `feature:*:impl` владеет экраном, UI state и пользовательскими командами Start/Stop;
- `service:monitoring:api` связывает фичи с процессным мониторингом;
- `service:monitoring:impl` владеет foreground service, scan job и notification lifecycle;
- `scanner:*` владеет Android framework API и не знает про UI или service lifecycle.

## Scanner API

Каждый scanner отдаёт данные через `Flow`. Сбор данных начинается при collection и останавливается при отмене collection.

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

Общая форма scanner events:

- `Scanning` — scanner запущен;
- `PermissionRequired` — caller должен запросить runtime permissions;
- `Unavailable` — отсутствует capability или выключена системная настройка;
- `Error` — platform API завершился ошибкой;
- результат — `Networks`, `Devices` или `Cells`.

## Диагностика Monitoring

Основные теги Logcat:

- `MonitoringController` — команды Start/Stop, permissions и изменения активных типов;
- `MonitoringService` — lifecycle service, foreground-режим и lifecycle scan job.

Фильтр через adb:

```shell
adb logcat -s MonitoringController MonitoringService
```

Ожидаемая проверка:

1. Start Wi-Fi: активируется `WIFI`, создаётся service и Wi-Fi job.
2. Переход на Bluetooth-вкладку: Wi-Fi job не останавливается.
3. Start Bluetooth: active types становятся `[WIFI, BLUETOOTH]`, запускается Bluetooth job.
4. Stop Wi-Fi: завершается только Wi-Fi job, Bluetooth продолжает работать.
5. Stop Bluetooth: active types становятся пустыми, notification удаляется и service завершается.
