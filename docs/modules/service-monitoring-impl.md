# Модуль `:service:monitoring:impl`

Модуль реализует foreground monitoring для Wi-Fi и Bluetooth. Это инфраструктурная прослойка без собственного экрана: фичи остаются владельцами пользовательских команд и представления данных.

## Основной поток

`DefaultMonitoringController` принимает Start/Stop, проверяет permissions, сохраняет active types и запускает `DeviceMonitoringService`. Service становится foreground, создаёт отдельную coroutine job для каждого активного типа, публикует scanner events обратно в controller и обновляет notification.

```text
MonitoringController state
  -> DeviceMonitoringService
  -> syncScanJobs(activeTypes)
  -> WifiScanner / BluetoothScanner
  -> publish event
  -> feature
```

При удалении одного типа отменяется только его job. После удаления последнего типа service снимает foreground-состояние, удаляет notification и вызывает `stopSelf()`.

## Файлы и классы

| Файл | Назначение |
|---|---|
| `DefaultMonitoringController.kt` | Хранит state и последние события, проверяет permissions, сохраняет active types и запускает service. |
| `DeviceMonitoringService.kt` | Управляет foreground lifecycle, scan jobs, notification и действиями `Stop all`. |
| `MonitoringServiceModule.kt` | Регистрирует controller как singleton под concrete и API типами. |
| `AndroidManifest.xml` | Объявляет service, foreground permissions и типы `location|connectedDevice`. |

## Controller

Active types сохраняются в `SharedPreferences` `monitoring_service`. При старте процесса controller восстанавливает их, а counts начинает с нуля. События хранятся в nullable `MutableStateFlow`; при явном удалении типа соответствующее последнее событие очищается.

Controller добавляет `POST_NOTIFICATIONS` к scanner permissions на Android 13+. Ошибка запуска foreground service откатывает новый active type и публикует typed error.

## Service

Service использует `SupervisorJob + Dispatchers.Main.immediate`, поэтому ошибка одной scan job не должна автоматически отменять другую. Wi-Fi interval выбирается по текущему throttling: 30 секунд при включённом и 5 секунд при выключенном. Bluetooth использует default config scanner, то есть режим `ALL`.

Notification показывает активные типы и количество результатов, открывает приложение по нажатию и содержит действие `Stop all`. Foreground service type вычисляется из active types: `location` для Wi-Fi и `connectedDevice` для Bluetooth.

Основные Logcat tags:

- `MonitoringController` — Start/Stop, permissions, active types и scanner events;
- `MonitoringService` — lifecycle service, foreground updates и scan jobs.

```shell
adb logcat -s MonitoringController MonitoringService
```

## Ограничения

Foreground service не обходит Bluetooth screen-off suspension, Wi-Fi throttling, BLE scan quota и OEM freezer. На Blackview возможен временный отказ `thirdAppBlacklisted`. `START_STICKY` не гарантирует restart после force stop и не восстанавливает данные, пропущенные во время остановки процесса. Полный список находится в [ограничениях платформы](../platform-limitations.md).
