# `:scanner-ui`

Compose-экраны для ручной проверки библиотеки. Модуль не обязателен для использования `:scanner`.

## Публичный API

```kotlin
@Composable fun WifiScanScreen(scanner: WifiScanner, modifier: Modifier = Modifier)
@Composable fun BluetoothScanScreen(scanner: BluetoothScanner, modifier: Modifier = Modifier)
```

Экран сам создаёт ViewModel через `viewModel { ... }`, запрашивает runtime-разрешения и сканирует после нажатия Start, пока экран виден. Всё остальное в модуле — `internal`.

## Содержимое

- `wifi/`: список сетей по убыванию RSSI, детали сети, диалог про scan throttling со ссылкой на Developer options.
- `bluetooth/`: сортировка `Stable` / `Signal strength` / `Name`, приглушение устаревших устройств, детали устройства с сырыми manufacturer data и service UUID. Сортировка по сигналу группирует RSSI по 5 dBm и внутри группы сохраняет текущий порядок, чтобы список не прыгал.
- `common/`: `ScanStatus`, запрос разрешений, скан на видимом экране и общие компоненты (`StatusCard`, `DetailRow`, `StartStopButtons`).

Раскладка файлов описана в [архитектуре](../architecture.md#scanner-ui).
