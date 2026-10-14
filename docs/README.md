# Документация проекта MonitoringDevices

Проект — переиспользуемая Android-библиотека мониторинга Wi-Fi и Bluetooth и тестовое приложение к ней.

## Основные страницы

- [Архитектура](architecture.md) — модули, раскладка пакетов, движение данных и переключение источников.
- [Ограничения платформы](platform-limitations.md) — фактическое поведение Wi-Fi и Bluetooth.
- [Исследование Android API](android-wireless-monitoring-research.md) — расширенный технический контекст и ссылки на внешние источники.

## Модули

- [`:monitoring`](modules/monitoring.md) — библиотека: API и сканеры.
- [`:monitoring-ui`](modules/monitoring-ui.md) — тестовые Compose-экраны поверх API.
- [`:app`](modules/app.md) — хост-приложение для ручной проверки.

## Подключение в другой проект

1. Подключить модуль `:monitoring` (и при необходимости `:monitoring-ui`).
2. Создать экземпляр `DeviceMonitoring(context, config)` и указать нужные источники в `enabledSources`.
3. Запросить разрешения из `missingPermissions(source)`, затем собирать `scanWifi()` / `scanBluetooth()`, пока экран виден.

Подробности — на странице [`:monitoring`](modules/monitoring.md).
