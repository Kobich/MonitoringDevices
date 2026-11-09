# Документация проекта MonitoringDevices

Проект — переиспользуемая фича сканирования Wi-Fi и Bluetooth (модуль `:features:nearby-scanner`) и приложение-хост для её проверки.

## Основные страницы

- [Архитектура сканера](architecture-scanner.md) — `:features:nearby-scanner`: раскладка пакетов, движение данных и правила именования.
- [Ограничения платформы](platform-limitations.md) — фактическое поведение Wi-Fi и Bluetooth.
- [Исследование Android API](android-wireless-monitoring-research.md) — расширенный технический контекст и ссылки на внешние источники.

## Модули

- [`:features:nearby-scanner`](modules/scanner.md) — фича: API, реализация сканеров и экраны.

## Подключение в другой проект

1. Подключить модуль `:features:nearby-scanner`.
2. Создать `NearbyScanner(context)` или только нужный сканер: `WifiScannerApi(context)`, `BluetoothScannerApi(context)`.
3. Открыть `NearbyScanScreen(scanner)` или работать со сканерами напрямую: запросить разрешения из `missingPermissions()` и собирать `scan()`, пока экран виден.

Подробности — на странице [`:features:nearby-scanner`](modules/scanner.md).
