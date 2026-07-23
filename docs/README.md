# Документация проекта MonitoringDevices

Документация разделена на обзор архитектуры, ограничения Android и страницы отдельных Gradle-модулей. Каждая страница самодостаточна и подходит для переноса в GitLab Wiki без переработки структуры текста.

## Основные страницы

- [Архитектура](architecture.md) — границы слоёв, зависимости и движение данных.
- [Ограничения платформы](platform-limitations.md) — фактическое поведение Wi-Fi, Bluetooth, Radio и foreground service.
- [Исследование Android API](android-wireless-monitoring-research.md) — расширенный технический контекст и ссылки на внешние источники.

## Модули

### Приложение

- [`:app`](modules/app.md)

### Wi-Fi

- [`:feature:wifi:api`](modules/feature-wifi-api.md)
- [`:feature:wifi:impl`](modules/feature-wifi-impl.md)
- [`:scanner:wifi:api`](modules/scanner-wifi-api.md)
- [`:scanner:wifi:impl`](modules/scanner-wifi-impl.md)

### Bluetooth

- [`:feature:bluetooth:api`](modules/feature-bluetooth-api.md)
- [`:feature:bluetooth:impl`](modules/feature-bluetooth-impl.md)
- [`:scanner:bluetooth:api`](modules/scanner-bluetooth-api.md)
- [`:scanner:bluetooth:impl`](modules/scanner-bluetooth-impl.md)

### Radio

- [`:feature:radio:api`](modules/feature-radio-api.md)
- [`:feature:radio:impl`](modules/feature-radio-impl.md)
- [`:scanner:radio:api`](modules/scanner-radio-api.md)
- [`:scanner:radio:impl`](modules/scanner-radio-impl.md)

### Фоновый мониторинг

- [`:service:monitoring:api`](modules/service-monitoring-api.md)
- [`:service:monitoring:impl`](modules/service-monitoring-impl.md)

## Как читать документацию

Для знакомства с проектом достаточно последовательно прочитать архитектуру, ограничения платформы и страницы интересующей вертикали. Например, Bluetooth проходит через `feature:bluetooth:*`, `service:monitoring:*` и `scanner:bluetooth:*`. Radio работает напрямую с scanner и не использует foreground service.
