# `:monitoring-ui`

Compose-экраны для ручной проверки библиотеки. Модуль не обязателен для использования `:monitoring`.

## Публичный API

```kotlin
@Composable fun WifiMonitoringScreen(monitoring: DeviceMonitoring, modifier: Modifier = Modifier)
@Composable fun BluetoothMonitoringScreen(monitoring: DeviceMonitoring, modifier: Modifier = Modifier)
```

Экран сам создаёт ViewModel через `viewModel { ... }`, запрашивает runtime-разрешения и показывает список с деталями по нажатию. Всё остальное в модуле — `internal`.

## Содержимое

- `wifi/`: список сетей по убыванию RSSI, детали сети, диалог про scan throttling со ссылкой на Developer options.
- `bluetooth/`: сортировка `Stable` / `Signal strength` / `Name`, приглушение устаревших устройств, детали устройства. Сортировка по сигналу группирует RSSI по 5 dBm и внутри группы сохраняет текущий порядок, чтобы список не прыгал.
- `common/`: запрос разрешений и общие компоненты (`StatusCard`, `DetailRow`, `StartStopButtons`).

Раскладка файлов описана в [архитектуре](../architecture.md#monitoring-ui).
