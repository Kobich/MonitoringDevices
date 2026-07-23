# Модуль `:scanner:wifi:api`

Модуль задаёт независимый от реализации контракт Wi-Fi scanner. Его используют feature и monitoring service; Android `WifiManager` в API не протекает.

## Контракт

`WifiScanner` публикует требуемые разрешения, текущий статус scan throttling и `scan(config)`. Сбор данных начинается при collection возвращённого `Flow`.

`WifiScanConfig` задаёт режим и необязательный интервал повторных активных запросов. Интервал должен быть положительным.

- `ACTIVE` запрашивает новое сканирование и слушает системные broadcasts.
- `CACHED_ONLY` один раз читает доступный cache и завершает Flow.

## События

`WifiScanEvent` содержит `Scanning`, `Networks`, `PermissionRequired`, `Unavailable` и `Error`. `WifiNetwork` хранит SSID, BSSID, RSSI, частоту и capabilities.

## Файлы

| Файл | Назначение |
|---|---|
| `WifiScanner.kt` | Scanner interface, config, режимы, события и модель сети. |

API не обещает, что каждое событие `Networks` получено новым физическим scan: Android может вернуть cached results.
