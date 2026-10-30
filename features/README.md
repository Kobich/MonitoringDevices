# Получение данных по старт-условиям

Библиотека для Android, которая получает данные об окружении устройства и проверяет по ним старт-условия.

## Описание

Модуль отдаёт данные из пяти источников — время, координаты, видимые Wi-Fi-сети, Bluetooth-устройства рядом и подключённые Bluetooth-устройства — и проверяет, выполняется ли по ним старт-условие. Каждая проверка отвечает `True`, `False` или `Unknown` с причиной, почему ответить нельзя: нет разрешения, источник выключен, данных нет и т.д.

Модуль сам не работает в фоне и не запрашивает разрешения: это задача приложения. Работу с фичами удобнее всего вести через `FeatureMediator` — одну точку входа для всех источников.

## Архитектура и стек

```
condition-data-fetcher/
├── src/main/kotlin/com/android/appmanager/feature/conditiondatafetcher/
│   ├── Feature.kt              # Общие интерфейсы, SourceId, ConditionResult, UnknownReason
│   ├── DataType.kt             # Данные getData: TimeData, GeoData, WifiFoundData, BtFoundData, BtConnectedData
│   ├── ConditionInput.kt       # Входы isMeetsCondition: TimeCondition, GeoCondition, WifiFoundCondition, ...
│   ├── FeatureMediator.kt      # Единая точка входа ко всем фичам
│   ├── time/                   # TimeFeature, TimeSource
│   ├── location/               # LocationFeature, LocationJudge (правила решения), GeoZone, LocationSample
│   ├── wifi/                   # WifiNetworksFeature, WifiNetwork
│   └── bluetooth/              # NearbyBluetoothDevicesFeature, BluetoothConnectedDevicesFeature и их модели
├── src/test/kotlin/...         # LocationJudgeTest, TimePeriodTest
├── src/main/AndroidManifest.xml
└── build.gradle.kts
```

Модуль состоит из трёх частей:

**Публичный интерфейс:**
- `Feature`, `DataFeature`, `ConditionFeature` — общие интерфейсы всех фич: `getId()`, `getData()`, `isMeetsCondition()`
- `FeatureMediator` — одна точка входа: отправляет запрос в нужную фичу по `SourceId` или по типу входа
- `DataType`, `ConditionInput` — sealed-иерархии данных и входов
- `ConditionResult`, `UnknownReason` — результат проверки и причины `Unknown`

**Фичи:**
- `TimeFeature` — время из самого надёжного источника: сеть, спутники, системное
- `LocationFeature` — координаты из fused, GPS и сети
- `WifiNetworksFeature` — Wi-Fi-сети из системного кеша
- `NearbyBluetoothDevicesFeature` — BLE-устройства рядом, свой короткий скан
- `BluetoothConnectedDevicesFeature` — подключённые сейчас Bluetooth-устройства

**Логика решения:**
- `LocationJudge` — правила для координат на чистом Kotlin, покрыты юнит-тестами

Модуль объявляет разрешения в `AndroidManifest.xml`, runtime-разрешения запрашивает приложение:

| Фича | Runtime-разрешения | Ещё нужно |
|---|---|---|
| `WifiNetworksFeature` | `ACCESS_FINE_LOCATION` | `ACCESS_WIFI_STATE` (выдаётся без запроса) |
| `BluetoothConnectedDevicesFeature` | `BLUETOOTH_CONNECT` | |
| `NearbyBluetoothDevicesFeature` | `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION` | `BLUETOOTH_CONNECT` по желанию: без него имя берётся только из эфира |
| `LocationFeature` | `ACCESS_FINE_LOCATION` (запрашивается вместе с `ACCESS_COARSE_LOCATION`) | в фоне — foreground service типа `location` |
| `TimeFeature` | нет | |

Без геолокации `NearbyBluetoothDevicesFeature` не работает: `BLUETOOTH_SCAN` объявлен без флага `neverForLocation`, и без разрешения на геолокацию Android молча выбрасывает все результаты скана. `WifiNetworksFeature` дополнительно требует включённые Wi-Fi и геолокацию: без них Android отдаёт пустой кеш вместо ошибки, поэтому фича проверяет их сама.

Технические зависимости: Kotlin Coroutines 1.9.0, JUnit (тестирование).

Минимальная версия SDK: 36. Версия компиляции SDK: 36.

## Подключение модуля к проекту

Добавьте модуль в список подпроектов в корневом файле `settings.gradle.kts`:

```kotlin
include(":manager-kit:features:condition-data-fetcher")
```

Добавьте зависимость в `build.gradle.kts` целевого модуля:

```kotlin
implementation(project(":manager-kit:features:condition-data-fetcher"))
```

## Описание API

### Общие интерфейсы

| Метод | Описание |
|---|---|
| `Feature.getId(): SourceId` | Идентификатор фичи: `TIME`, `GEO`, `WIFI_FOUND`, `BT_FOUND`, `BT_CONNECTED` |
| `DataFeature.getData(): T?` | Данные источника или `null`, если получить их нельзя |
| `ConditionFeature.isMeetsCondition(input: P): ConditionResult` | Проверка условия: `True`, `False` или `Unknown(reason)` |

### FeatureMediator

| Метод | Описание |
|---|---|
| `FeatureMediator(context)` | Создаёт все фичи сам |
| `FeatureMediator(time, geo, wifiFound, btFound, btConnected)` | Принимает готовые фичи, например из DI |
| `getData(id: SourceId): DataType?` | Данные фичи с этим `id` |
| `isMeetsCondition(input: ConditionInput): ConditionResult` | Проверка в фиче, которая отвечает за этот вход |
| `features: Map<SourceId, Feature>` | Все фичи по `id` |

### Фичи, входы и данные

| Фича | `getId()` | Вход `isMeetsCondition` | `getData` | Время |
|---|---|---|---|---|
| `TimeFeature` | `TIME` | `TimeCondition(periods: List<TimePeriod(start, end)>)` — строки ISO 8601 | `TimeData(instant, source)` — всегда | мгновенно |
| `LocationFeature` | `GEO` | `GeoCondition(zones: List<GeoZone>)` | `GeoData(latitude, longitude, accuracyMeters, source, ageMillis)` | сразу по последней точке или до 20 сек |
| `WifiNetworksFeature` | `WIFI_FOUND` | `WifiFoundCondition(networks: List<WifiNetworkCondition(name, bssid?)>)` | `WifiFoundData(networks: List<WifiNetwork>)` — из системного кеша | мгновенно, скан не запускает |
| `NearbyBluetoothDevicesFeature` | `BT_FOUND` | `BtFoundCondition(names: List<String>)` | `BtFoundData(devices: List<NearbyBluetoothDevice>)` | скан до 10 сек, не чаще раза в 30 сек |
| `BluetoothConnectedDevicesFeature` | `BT_CONNECTED` | `BtConnectedCondition(names: List<String>)` | `BtConnectedData(devices: List<BluetoothConnectedDevice>)` | до 2 сек на профиль |

Условие выполняется, если подходит **хотя бы одно** значение из списка. Пустой список сразу даёт `False`, данные при этом не читаются.

Wi-Fi-сеть подходит, если совпало имя; `bssid` проверяется, только если он указан.

`NearbyBluetoothDevicesFeature.getData()` сканирует без фильтра, а такой скан при выключенном экране ничего не получает. Поэтому в фоне он вернёт `BtFoundData` с пустым списком, хотя устройства рядом могут быть. Для фоновых проверок используйте `isMeetsCondition`: он сканирует с фильтром по именам и работает при выключенном экране.

### Причины `Unknown`

| `UnknownReason` | Когда | Фичи |
|---|---|---|
| `PERMISSION_DENIED` | не выдано runtime-разрешение | все, кроме Time |
| `SOURCE_DISABLED` | выключен Wi-Fi, Bluetooth, геолокация или все её источники | Wi-Fi, Bluetooth, Location |
| `SOURCE_UNAVAILABLE` | на устройстве нет нужного железа или сервиса | все, кроме Time |
| `TOO_FREQUENT` | предыдущий скан запущен меньше 30 сек назад | Nearby Bluetooth |
| `READ_FAILED` | система вернула ошибку при чтении или скане | Bluetooth |
| `NO_DATA` | данных нет: кеш Wi-Fi пуст, нет ни одной координаты не старше 2 минут | Wi-Fi, Location |
| `NOT_ACCURATE_ENOUGH` | пользователь у границы зоны, погрешность не даёт решить | Location |
| `NOT_VERIFIED` | есть только время, выставленное вручную, и оно внутри хотя бы одного периода | Time |

### LocationFeature

Защиты от подмены координат нет: цель — отдавать координату там, где Android её знает.

Использует все включённые источники:

* **Fused** — объединённая координата Android: спутники, Wi-Fi, вышки и датчики. Обычно лучшая, в том числе в помещении.
* **GPS** — только спутники.
* **Сеть** — по видимым Wi-Fi-точкам и сотовым вышкам.

Как работает вызов:

1. Нет разрешения → `PERMISSION_DENIED`. Геолокация выключена → `SOURCE_DISABLED`. Ни один источник не включён → `SOURCE_DISABLED` (или `SOURCE_UNAVAILABLE`, если их нет на устройстве).
2. Сразу берём последнюю известную точку каждого источника, ответ может прийти мгновенно.
3. Если её не хватило, ждём новые точки, по умолчанию до 20 сек: `LocationFeature(context, window = 60.seconds)`. Без SIM-карты и интернета первая точка GPS на улице может появиться только через 1–3 минуты, 20 сек на это не хватит.
4. Точки старше 2 минут не используем. У остальных считаем погрешность с запасом: погрешность Android × 2 (уверенность около 95% вместо 68%) плюс путь, который человек мог пройти пешком с момента замера (1,5 м/с). Старая точка не выбрасывается, а просто становится менее уверенной.
5. Используется самая точная точка на текущий момент.

`getData()` возвращает `GeoData`: широта, долгота, погрешность, источник и возраст точки. Отвечает, как только есть точка не старше 10 сек, иначе по истечении 20 сек отдаёт лучшую из тех, что есть, или `null`.

`isMeetsCondition(GeoCondition(zones))` — внутри ли пользователь хотя бы одной зоны. Для зоны с радиусом `r`, расстоянием до центра `d` и погрешностью с запасом `a`:

* `d + a ≤ r` → `True`, сразу;
* `d − a > r` → `False`, сразу;
* иначе ждём точнее. Через 20 сек без уверенного ответа → `NOT_ACCURATE_ENOUGH`, а если точек нет совсем → `NO_DATA`.

Правила лежат в `LocationJudge` (чистый Kotlin) и покрыты тестами `LocationJudgeTest`.

`LocationFeature(context, trace = { line -> ... })` пишет в `trace` каждый шаг: какие источники включены, сколько спутников GPS видит и использует, каждую точку (погрешность, возраст, погрешность с запасом, расстояние до зоны) и итог со временем. Подходит для логов и отладки.

В фоне фиче нужен foreground service типа `location`.

### TimeFeature

Берёт время из самого надёжного доступного источника, по очереди:

1. **Сетевое время** (NTP).
2. **Спутниковое время** (GPS).
3. **Системное время** при включённом «Автоматически» (`SYSTEM`). Это уступка: пользователь может обойти и автоматическое время.
4. **Время телефона, выставленное вручную** (`MANUAL`), — последний вариант, оно может быть каким угодно.

Сетевое и спутниковое время система запоминает при синхронизации и дальше отсчитывает по внутреннему таймеру, поэтому ручной перевод часов на них не влияет. После перезагрузки они недоступны до следующей синхронизации.

* `getData()` — всегда возвращает время и источник (`NETWORK`, `GNSS`, `SYSTEM`, `MANUAL`). Насколько ему доверять, видно по источнику.
* `isMeetsCondition(TimeCondition(periods))` — текущее время входит хотя бы в один период. Период: от `start` включительно до `end` не включительно.

  Строки в формате ISO 8601 со смещением часового пояса: `+03:00`, `Z` для UTC, можно добавить пояс `[Europe/Moscow]`. Без смещения строка не принимается: непонятно, о каком поясе речь. Пояс телефона не используется.
* Если есть только время, выставленное вручную: вне всех периодов → `False`, внутри хотя бы одного → `Unknown(NOT_VERIFIED)`, потому что ручному времени не доверяем.

## Примеры использования

### Через медиатор

```kotlin
val mediator = FeatureMediator(context) // или FeatureMediator(time, geo, wifiFound, btFound, btConnected) из DI

mediator.isMeetsCondition(
    TimeCondition(listOf(TimePeriod("2026-10-24T09:00:00+03:00", "2026-10-24T18:00:00+03:00")))
)
mediator.isMeetsCondition(WifiFoundCondition(listOf(WifiNetworkCondition(name = "Office"))))
mediator.isMeetsCondition(GeoCondition(listOf(GeoZone(latitude = 55.7558, longitude = 37.6173, radiusMeters = 100))))

val location = mediator.getData(SourceId.GEO) as? GeoData
```

### Прямой вызов фичи

```kotlin
val wifi = WifiNetworksFeature(context)
when (val result = wifi.isMeetsCondition(WifiFoundCondition(listOf(WifiNetworkCondition("Office", bssid = "aa:bb:cc:dd:ee:ff"))))) {
    ConditionResult.True -> { /* сеть рядом */ }
    ConditionResult.False -> { /* сети нет */ }
    is ConditionResult.Unknown -> log(result.reason)
}
```

### Лог шагов определения координат

```kotlin
val location = LocationFeature(context, trace = { line -> logger.d("Location", line) })
```
