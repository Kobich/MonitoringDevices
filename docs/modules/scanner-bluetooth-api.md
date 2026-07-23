# Модуль `:scanner:bluetooth:api`

Модуль задаёт контракт Bluetooth scanner и общую модель BLE/Classic устройств. Feature и monitoring service зависят от этого API, не импортируя Android Bluetooth classes.

## Контракт

`BluetoothScanner` публикует набор разрешений и `scan(config)`. `BluetoothScanConfig` выбирает один из режимов:

- `BLE` — только Bluetooth Low Energy advertising;
- `CLASSIC` — только Classic discovery;
- `ALL` — попытка запуска обоих механизмов, используется по умолчанию.

## События и модели

`BluetoothScanEvent` содержит `Scanning`, `Devices`, `PermissionRequired`, `Unavailable` и `Error`.

`BluetoothDeviceInfo` хранит имя, адрес, RSSI, тип устройства, bond state и возраст последнего BLE-наблюдения. Типы нормализованы в `BluetoothDeviceType`, состояние pairing — в `BluetoothBondState`.

## Файлы

| Файл | Назначение |
|---|---|
| `BluetoothScanner.kt` | Scanner interface, config, режимы, события и модели устройства. |

Адрес не является гарантированно стабильным идентификатором: BLE peripheral может использовать rotating private address.
