# `:monitoring-ui`

Compose-экраны для ручной проверки библиотеки. Модуль не обязателен для использования `:monitoring`.

## Публичный API

```kotlin
@Composable fun WifiMonitoringScreen(monitoring: DeviceMonitoring, modifier: Modifier = Modifier)
@Composable fun BleMonitoringScreen(monitoring: DeviceMonitoring, modifier: Modifier = Modifier)
```

Экран сам создаёт ViewModel через `viewModel { ... }`, запрашивает runtime-разрешения и показывает список с деталями по нажатию.

## Содержимое

- Wi-Fi: список сетей по убыванию RSSI, детали сети, диалог про scan throttling со ссылкой на Developer options.
- BLE: сортировка `Stable` / `Signal strength` / `Name`, приглушение устаревших устройств, детали устройства.
- Сортировка по сигналу группирует RSSI по 5 dBm и внутри группы сохраняет текущий порядок, чтобы список не прыгал.

Внутренние классы (`*ViewModel`, `*UiStateReducer`, `BleDeviceUiMapper`, UI-модели) закрыты `internal`.
