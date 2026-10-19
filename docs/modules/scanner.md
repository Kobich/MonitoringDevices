# `:scanner`

Android library со всей фичей: сканеры Wi-Fi и Bluetooth и их экраны на Compose. DI-фреймворк не нужен. Фоновой работы нет: сканирование идёт, пока собирается Flow.

Раскладка пакетов описана в [архитектуре](../architecture.md#как-ориентироваться-в-scanner).

## Публичный API

| Тип | Пакет | Назначение |
|---|---|---|
| `NearbyScanner`, `NearbyScannerConfig` | корень | API фичи: объединяет оба сканера и их конфигурацию |
| `WifiScannerApi`, `ScannedWifiNetwork`, `WifiScanEvent`, `WifiUnavailableReason`, `WifiScanConfig` | `wifi` | Сканер Wi-Fi, его модели, события и параметры |
| `BluetoothScannerApi`, `ScannedBluetoothDevice`, `BluetoothScanEvent`, `BluetoothUnavailableReason`, `BluetoothScanConfig` | `bluetooth` | Сканер Bluetooth, его модели, события и параметры |
| `NearbyScanScreen` | `ui` | Экран фичи с вкладками Wi-Fi и Bluetooth |
| `WifiScanScreen`, `BluetoothScanScreen` | `ui.wifi`, `ui.bluetooth` | Экраны отдельных сканеров, если нужен только один |

Причины недоступности — enum, тексты для пользователя формирует UI.

## Использование

Открыть экран фичи:

```kotlin
NearbyScanScreen(scanner = NearbyScanner(applicationContext))
```

Использовать сканер напрямую, без UI фичи:

```kotlin
val bluetooth = NearbyScanner(applicationContext).bluetooth

val missing = bluetooth.missingPermissions()
if (missing.isNotEmpty()) {
    requestPermissions(missing)
} else {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            bluetooth.scan().collect { event -> /* Scanning, Devices, ... */ }
        }
    }
}
```

## Манифест

Модуль объявляет только разрешения сканирования обоих сканеров. Если приложение использует один сканер, разрешения другого можно убрать в манифесте приложения через `tools:node="remove"`.
