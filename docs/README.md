# Документация проекта MonitoringDevices

Проект — переиспользуемая Android-библиотека мониторинга Wi-Fi и Bluetooth LE и тестовое приложение к ней.

## Основные страницы

- [Архитектура](architecture.md) — модули, публичный API, движение данных и переключение источников.
- [Ограничения платформы](platform-limitations.md) — фактическое поведение Wi-Fi, BLE и foreground service.
- [Исследование Android API](android-wireless-monitoring-research.md) — расширенный технический контекст и ссылки на внешние источники.

## Модули

- [`:monitoring`](modules/monitoring.md) — библиотека: API, сканеры, foreground service.
- [`:monitoring-ui`](modules/monitoring-ui.md) — тестовые Compose-экраны поверх API.
- [`:app`](modules/app.md) — хост-приложение для ручной проверки.

## Подключение в другой проект

1. Подключить модуль `:monitoring` (и при необходимости `:monitoring-ui`).
2. В `Application.onCreate` вызвать `DeviceMonitoring.initialize(context, config)` и указать нужные источники в `enabledSources`.
3. Запрашивать runtime-разрешения из `requiredPermissions(source)`, затем вызывать `start(source)` и собирать `wifiEvents()` / `bleEvents()`.

Подробности — на странице [`:monitoring`](modules/monitoring.md).
