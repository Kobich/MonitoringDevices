# Модуль `:scanner:wifi:impl`

Модуль реализует `WifiScanner` через `WifiManager`. Он проверяет разрешения и системные условия, читает scan cache, запускает активные запросы и освобождает receiver при отмене Flow.

## Работа scanner

В `CACHED_ONLY` scanner выполняет одну проверку и одно чтение `WifiManager.scanResults`. В `ACTIVE` он сначала отправляет текущее содержимое cache, регистрирует receiver на `SCAN_RESULTS_AVAILABLE_ACTION`, вызывает `startScan()` и при наличии интервала повторяет запросы.

Если `startScan()` возвращает `false`, scanner повторно читает cache. Непустой cache публикуется как результат; при пустом cache возвращается диагностическая причина и Flow закрывается.

## Файлы и классы

| Файл | Назначение |
|---|---|
| `AndroidWifiScanner.kt` | Координирует режимы scan, BroadcastReceiver, polling job и события Flow. |
| `WifiScanPermissions.kt` | Формирует набор разрешений для текущей Android-версии и находит отсутствующие. |
| `WifiScanDiagnostics.kt` | Проверяет Wi-Fi, Location Services и формирует причины отказа. |
| `WifiScanResultReader.kt` | Преобразует Android `ScanResult` в `WifiNetwork`, включая version-safe чтение SSID. |
| `WifiScanThrottlingStatusReader.kt` | Читает системный статус Wi-Fi scan throttling; при ошибке безопасно считает throttling включённым. |
| `WifiScannerModule.kt` | Регистрирует singleton `WifiScanner` в Koin. |
| `AndroidManifest.xml` | Объявляет Wi-Fi, location и nearby Wi-Fi permissions. |

## Lifecycle

Receiver и polling job существуют только пока Flow собирается. `awaitClose` отменяет job и снимает receiver. Scanner использует application context и не удерживает Activity.

## Ограничения

`WifiManager.startScan()` является ограниченным платформой API. Успешный вызов не гарантирует свежий результат, а `false` не всегда означает окончательную ошибку. Интервалы monitoring service и системный throttling описаны в [ограничениях платформы](../platform-limitations.md).
