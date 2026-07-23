# Модуль `:scanner:radio:api`

Модуль задаёт контракт чтения информации о сотовых cells. Android telephony classes не выходят за границу реализации.

## Контракт

`RadioScanner` публикует требуемые разрешения и `scan(config)`. Доступны два режима:

- `REGISTERED_ONLY` возвращает только serving/registered cells;
- `ALL_AVAILABLE` возвращает все cells, предоставленные модемом, и используется по умолчанию.

## События и модели

`RadioScanEvent` содержит `Scanning`, `Cells`, `PermissionRequired`, `Unavailable` и `Error`.

`RadioCellInfo` хранит нормализованный network type, registration state, dBm и имя оператора. Поддерживаются GSM, CDMA, WCDMA, LTE, NR и `UNKNOWN`.

## Файлы

| Файл | Назначение |
|---|---|
| `RadioScanner.kt` | Scanner interface, config, режимы, события и модель cell. |

Контракт описывает снимок доступных данных, а не непрерывную подписку на modem callbacks.
