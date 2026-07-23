# Модуль `:service:monitoring:api`

Модуль задаёт контракт между Wi-Fi/Bluetooth фичами и процессным слоем мониторинга. Он не содержит Android `Service`, notification и scanner lifecycle.

## Контракт

`MonitoringController` публикует:

- `state` с активными типами и количеством результатов;
- `wifiEvents` и `bluetoothEvents`;
- команды запуска, остановки одного типа и остановки всех типов.

`startMonitoring(type)` возвращает набор отсутствующих разрешений. Пустой набор означает, что запрос принят; фактический результат запуска приходит через event flow.

## Модели

`MonitoringType` содержит `WIFI` и `BLUETOOTH`. Radio намеренно отсутствует, потому что не использует foreground service.

`MonitoringState` хранит active types, количество Wi-Fi сетей и Bluetooth устройств. Состояние описывает намерение monitoring-слоя, а не гарантирует, что OEM в текущий момент не приостановил platform scan.

## Файлы

| Файл | Назначение |
|---|---|
| `MonitoringController.kt` | Controller interface, типы мониторинга и state model. |

Модуль экспортирует scanner event types через зависимости на `scanner:wifi:api` и `scanner:bluetooth:api`.
