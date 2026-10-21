# scanner

## Описание

Сканирует ближайшие Wi-Fi сети и Bluetooth-устройства и показывает их на готовом экране. Фича рассчитана на короткую сессию на открытом экране: пользователь открывает экран, смотрит, что вокруг, и уходит; в фоне ничего не работает. Модуль самодостаточный: приложение-хост добавляет его как один destination в свой граф Compose Navigation.

## Архитектура и стек

* **Слои:** Data (`wifi/`, `bluetooth/` — сканеры поверх Android API) и Presentation (`ui/` — Compose-экраны и ViewModel). Отдельного Domain-слоя нет: сканеры отдают события, которые UI напрямую сводит в состояние.
* **Ключевые технологии:**
  * Kotlin Coroutines / Flow 1.8.1
  * Jetpack Compose (BOM 2026.02.01), Material 3
  * Navigation Compose 2.6.0
  * Lifecycle (ViewModel, `repeatOnLifecycle`, `collectAsStateWithLifecycle`) 2.6.1
  * Activity Compose 1.9.3, Core KTX 1.17.0
  * Android API: `WifiManager`, `BluetoothLeScanner`
  * SDK: `minSdk` 36, `compileSdk` 36.1 (только Android 16)
  * Сборка (из общего проекта): Android Gradle Plugin 9.2.0, Kotlin 2.4.21
* **Зависимости:** от модулей `:core` и `:features` не зависит. Только внешние зависимости: Kotlin Coroutines (api), Navigation Compose (api), Compose, Lifecycle и Activity Compose (implementation).

Структура пакетов:

```text
com.engboost.monitoringdevices.scanner
├── NearbyScanner.kt / NearbyScannerConfig.kt   Точка входа фичи и конфигурация
├── wifi/        WifiScannerApi, ScannedWifiNetwork, WifiScanEvent, WifiScanConfig; WifiScannerImpl (internal)
├── bluetooth/   BluetoothScannerApi, ScannedBluetoothDevice, BluetoothScanEvent, BluetoothScanConfig;
│                BluetoothScannerImpl, BluetoothDeviceRegistry (internal)
└── ui/          NearbyScanScreen, хелперы навигации; wifi/, bluetooth/, common/ (internal)
```

## Описание API

### Навигация

| Объявление | Описание |
|---|---|
| `NEARBY_SCAN_ROUTE` | Маршрут экрана фичи: `"nearby_scan"`. |
| `NavGraphBuilder.nearbyScanScreen(scanner: NearbyScanner)` | Добавляет экран фичи в граф хоста. |
| `NavController.navigateToNearbyScan(navOptions: NavOptions? = null)` | Открывает экран фичи. |
| `@Composable NearbyScanScreen(scanner, modifier)` | Сам экран фичи с вкладками Wi-Fi и Bluetooth. |

Сканирование начинается после нажатия Start и идёт, только пока экран виден. Runtime-разрешения запрашивает сам экран.

### Сканеры

| Объявление | Описание |
|---|---|
| `NearbyScanner(context, config = NearbyScannerConfig())` | Создаёт оба сканера. Основной конструктор `NearbyScanner(wifi, bluetooth)` принимает фейки. |
| `WifiScannerApi(context, config)`, `BluetoothScannerApi(context, config)` | Создают один сканер, если приложению нужен только он. |
| `requiredPermissions: Set<String>` | Runtime-разрешения, нужные сканеру. |
| `missingPermissions(): Set<String>` | Разрешения, которые ещё не выданы. |
| `scan(): Flow<…ScanEvent>` | Холодный Flow: сканирует, пока его собирают, и сам завершается только после терминального события. |
| | Bluetooth-скан запускается не чаще раза в 8 секунд на всё приложение: Android разрешает 5 запусков BLE-скана за 30 секунд, и один запуск остаётся для скана мониторинга. Слишком ранний запуск откладывается, а не отбрасывается. |
| `WifiScannerApi.isScanThrottlingEnabled: Boolean` | Включено ли ограничение частоты Wi-Fi-сканов (scan throttling) в настройках разработчика. |

### Выходные данные

| Событие | Значение |
|---|---|
| `WifiScanEvent.Scanning`, `BluetoothScanEvent.Scanning` | Скан запущен. |
| `WifiScanEvent.Networks(List<ScannedWifiNetwork>)` | Сети последнего скана: `ssid` (`null` для скрытой сети), `bssid`, `rssiDbm`, `frequencyMhz`, `capabilities`. |
| `BluetoothScanEvent.Devices(List<ScannedBluetoothDevice>)` | Полный список устройств, а не изменения: `name` (`null`, если имя не транслируется), `address`, сглаженный `rssiDbm`, `type`, `bondState`, `lastSeenAgoMillis`, `isStale`, сырые `manufacturerData` (ID компании → payload в hex), `serviceUuids`. |

### Ошибки

Все события ниже терминальные: после них Flow завершается.

| Событие | Причины / детали |
|---|---|
| `PermissionRequired(permissions)` | Не выданы какие-то из `requiredPermissions`. |
| `WifiScanEvent.Unavailable(reason)` | `WIFI_DISABLED`, `LOCATION_DISABLED`, `SCAN_REJECTED` (Android отклонил запрос, а кэшированных результатов нет). |
| `BluetoothScanEvent.Unavailable(reason)` | `ADAPTER_UNAVAILABLE`, `BLUETOOTH_DISABLED`, `LE_SCANNER_UNAVAILABLE`. |
| `Error(message, cause)` | Непредвиденный сбой. Для Bluetooth в сообщении есть код ошибки `ScanCallback`, например `Bluetooth LE scan failed: 2`. |

### Конфигурация

| Параметр | По умолчанию | Значение |
|---|---|---|
| `WifiScanConfig.throttledRefreshIntervalMillis` | 30 000 | Интервал сканов, когда scan throttling включён. |
| `WifiScanConfig.unthrottledRefreshIntervalMillis` | 5 000 | Интервал сканов, когда throttling выключен. |
| `BluetoothScanConfig.snapshotIntervalMillis` | 750 | Список устройств публикуется не чаще этого интервала. |
| `BluetoothScanConfig.ageRefreshIntervalMillis` | 5 000 | Как часто обновляется возраст устройств и удаляются устаревшие. |
| `BluetoothScanConfig.staleAfterMillis` | 30 000 | Устройство, которого не видно дольше, получает `isStale = true`. |
| `BluetoothScanConfig.retentionMillis` | 120 000 | Устройство, которого не видно дольше, удаляется из списка. |
| `BluetoothScanConfig.rssiSmoothingFactor` | 0.25 | Экспоненциальное сглаживание RSSI, `1` отключает его. |
| `BluetoothScanConfig.minRssiChangeDbm` | 3 | Изменения RSSI меньше этого значения не публикуются. |

### Разрешения

Объявлены в манифесте модуля и попадают в приложение-хост при слиянии манифестов:

* Wi-Fi: `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `NEARBY_WIFI_DEVICES`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`.
* Bluetooth: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`.

## Примеры использования

Подключить модуль:

```kotlin
// build.gradle.kts приложения-хоста
dependencies {
    implementation(project(":scanner"))
}
```

Создать сканер один раз, например в `Application` или в DI-контейнере:

```kotlin
class App : Application() {
    val scanner: NearbyScanner by lazy { NearbyScanner(this) }
}
```

Зарегистрировать экран в графе навигации хоста и открыть его:

```kotlin
NavHost(navController = navController, startDestination = "home") {
    composable("home") {
        HomeScreen(onOpenScannerClick = { navController.navigateToNearbyScan() })
    }
    nearbyScanScreen(scanner = scanner)
}
```

Использовать сканер без UI фичи:

```kotlin
val bluetooth = BluetoothScannerApi(context)

if (bluetooth.missingPermissions().isEmpty()) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            bluetooth.scan().collect { event ->
                when (event) {
                    is BluetoothScanEvent.Devices -> show(event.devices)
                    is BluetoothScanEvent.Unavailable -> showReason(event.reason)
                    else -> Unit
                }
            }
        }
    }
}
```
