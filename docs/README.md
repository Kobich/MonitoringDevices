# Документация проекта MonitoringDevices

Проект — переиспользуемая фича сканирования Wi-Fi и Bluetooth (модуль `:scanner`) и приложение-хост для её проверки.

## Основные страницы

- [Архитектура](architecture.md) — модули, раскладка пакетов, движение данных и правила именования.
- [Ограничения платформы](platform-limitations.md) — фактическое поведение Wi-Fi и Bluetooth.
- [Исследование Android API](android-wireless-monitoring-research.md) — расширенный технический контекст и ссылки на внешние источники.

## Модули

- [`:scanner`](modules/scanner.md) — фича: API, реализация сканеров и экраны.
- [`:app`](modules/app.md) — хост-приложение для ручной проверки.

## Подключение в другой проект

1. Подключить модуль `:scanner`.
2. Создать `NearbyScanner(context)` или только нужный сканер: `WifiScannerApi(context)`, `BluetoothScannerApi(context)`.
3. Открыть `NearbyScanScreen(scanner)` или работать со сканерами напрямую: запросить разрешения из `missingPermissions()` и собирать `scan()`, пока экран виден.

Подробности — на странице [`:scanner`](modules/scanner.md).
