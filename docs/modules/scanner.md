# `:scanner`

Android library со всей логикой сканирования Wi-Fi и Bluetooth. Зависит только от `kotlinx-coroutines`; Compose, androidx и DI-фреймворк не нужны. Фоновой работы нет: сканирование идёт, пока собирается Flow.

Раскладка пакетов описана в [архитектуре](../architecture.md#как-ориентироваться-в-scanner).

## Публичный API

| Тип | Пакет | Назначение |
|---|---|---|
| `NearbyScanner`, `NearbyScannerConfig` | корень | API фичи: объединяет оба сканера и их конфигурацию |
| `WifiScannerApi`, `ScannedWifiNetwork`, `WifiScanEvent`, `WifiUnavailableReason`, `WifiScanConfig` | `wifi` | Сканер Wi-Fi, его модели, события и параметры |
| `BluetoothScannerApi`, `ScannedBluetoothDevice`, `BluetoothScanEvent`, `BluetoothUnavailableReason`, `BluetoothScanConfig` | `bluetooth` | Сканер Bluetooth, его модели, события и параметры |

Причины недоступности — enum, тексты для пользователя формирует UI.

## Использование

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
