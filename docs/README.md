# Документация проекта MonitoringDevices

Проект — переиспользуемая Android-библиотека сканирования Wi-Fi и Bluetooth и тестовое приложение к ней.

## Основные страницы

- [Архитектура](architecture.md) — модули, раскладка пакетов, движение данных и правила именования.
- [Ограничения платформы](platform-limitations.md) — фактическое поведение Wi-Fi и Bluetooth.
- [Исследование Android API](android-wireless-monitoring-research.md) — расширенный технический контекст и ссылки на внешние источники.

## Модули

- [`:scanner`](modules/scanner.md) — библиотека: API и реализация сканеров.
- [`:scanner-ui`](modules/scanner-ui.md) — тестовые Compose-экраны поверх API.
- [`:app`](modules/app.md) — хост-приложение для ручной проверки.

## Подключение в другой проект

1. Подключить модуль `:scanner` (и при необходимости `:scanner-ui`).
2. Создать `NearbyScanner(context)` или только нужный сканер: `WifiScannerApi(context)`, `BluetoothScannerApi(context)`.
3. Запросить разрешения из `missingPermissions()`, затем собирать `scan()`, пока экран виден.

Подробности — на странице [`:scanner`](modules/scanner.md).
