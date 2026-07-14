# Исследование: мониторинг Wi-Fi, Bluetooth и радиосигналов на Android

Актуально на: 14 июля 2026.

## Краткий вывод

На обычном Android-устройстве без root и без кастомной прошивки можно делать прикладной мониторинг ближайших беспроводных сущностей: Wi-Fi точек доступа через `WifiManager`, BLE-реклам через `BluetoothLeScanner`, Bluetooth Classic устройств через discovery, параметров сотовой сети через `TelephonyManager`, а также расстояний до совместимых Wi-Fi RTT/UWB устройств. Это подходит для приложения, которое показывает окружение устройства, уровень сигнала, историю появления/исчезновения устройств, приблизительную близость и диагностические статусы.

Нельзя рассчитывать на полноценный радиомониторинг в смысле spectrum analyzer, packet sniffer или пассивный перехват кадров 802.11/Bluetooth/baseband. Стандартные Android API не дают сырые IQ-сэмплы, не открывают monitor mode Wi-Fi-чипа, не дают читать произвольные Bluetooth-пакеты и сильно ограничивают фоновые сканирования из-за приватности, батареи и безопасности. Для настоящего RF-мониторинга нужен внешний приемник: например USB/Bluetooth/Wi-Fi SDR, специализированный BLE/Wi-Fi sniffer или отдельный датчик, с которым Android-приложение работает как UI, сборщик и транспорт данных.

## Что считать мониторингом

В Android важно разделять четыре уровня мониторинга.

| Уровень | Что можно получить | Типичный API | Реалистично без root |
|---|---|---|---|
| Обнаружение | Список видимых Wi-Fi AP, BLE advertising устройств, Bluetooth Classic устройств, paired devices | `WifiManager`, `BluetoothLeScanner`, `BluetoothAdapter` | Да |
| Метрики сигнала | RSSI, частота/канал, capabilities, PHY, timestamp, параметры cell signal | `ScanResult`, `ScanResult` BLE, `CellInfo`, `SignalStrength` | Да, с разрешениями и ограничениями |
| Дистанция/позиционирование | Wi-Fi RTT distance, UWB ranging, косвенная близость по RSSI | `WifiRttManager`, UWB APIs, BLE RSSI | Частично, зависит от железа |
| Сырые радиоданные | 802.11 frames, Bluetooth link-layer packets, IQ samples, спектр | Monitor mode, SDR, vendor/debug API | Нет на stock Android |

Для проекта мониторинга лучше проектировать систему вокруг первого и второго уровней, а третий и четвертый делать как опциональные расширения под конкретное железо.

## Wi-Fi

### Возможности

Android предоставляет Wi-Fi scanning через `WifiManager`. Базовый сценарий состоит из регистрации receiver на `WifiManager.SCAN_RESULTS_AVAILABLE_ACTION`, вызова `WifiManager.startScan()` и чтения результатов через `WifiManager.getScanResults()`. Начиная с Android 10 broadcast о завершении полного Wi-Fi-скана приходит не только на скан, запрошенный самим приложением, но и на сканы, выполненные платформой или другими приложениями. Это позволяет пассивно слушать обновления scan cache, не дергая `startScan()` постоянно.

Из `ScanResult` обычно полезны:

- `SSID`/`wifiSsid`, если доступно;
- `BSSID`, то есть MAC точки доступа;
- `level`, RSSI в dBm;
- `frequency`, канал можно вычислить из частоты;
- `capabilities`, например WPA/WPA2/WPA3 признаки;
- `timestamp`, время измерения;
- ширина канала, center frequency, Wi-Fi standard, если доступны на целевой API/устройстве.

Для мониторингового приложения этого достаточно, чтобы строить карту видимых сетей, историю RSSI, оценивать стабильность окружения, фиксировать появление/исчезновение точек доступа и диагностировать плотность эфира по каналам. Но RSSI не является точной дистанцией: он зависит от антенн, корпуса телефона, ориентации, препятствий, мощности AP, multipath и прошивки.

### Разрешения и системные условия

Для современных Android версий нужны одновременно manifest permissions, runtime permissions и включенные системные настройки.

На Android 10+ успешные `startScan()` и `getScanResults()` требуют location-доступа и включенного Location toggle на устройстве. Для `startScan()` также нужен `CHANGE_WIFI_STATE`, для `getScanResults()` нужен `ACCESS_WIFI_STATE`. Для приложений, targetSdk которых Android 10 или выше, требуется `ACCESS_FINE_LOCATION`.

С Android 13 появился `NEARBY_WIFI_DEVICES`, который относится к группе Nearby devices. Его нужно объявлять для ряда Wi-Fi API, включая Wi-Fi Aware, Wi-Fi Direct, LocalOnlyHotspot и Wi-Fi RTT. Но важная деталь: даже если приложение target Android 13+, `WifiManager.startScan()` и `WifiManager.getScanResults()` по официальной документации все еще требуют `ACCESS_FINE_LOCATION`. Поэтому для простого мониторинга видимых Wi-Fi сетей нельзя заменить location permission только на `NEARBY_WIFI_DEVICES`.

Минимальный manifest для Wi-Fi scanning на современных версиях обычно выглядит так:

```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Нужен для части nearby Wi-Fi API на Android 13+, но не заменяет location для startScan/getScanResults. -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
```

Если приложение использует Wi-Fi API, но не выводит физическую геолокацию, для `NEARBY_WIFI_DEVICES` можно указать `android:usesPermissionFlags="neverForLocation"`. Для scan results это обычно не избавляет от `ACCESS_FINE_LOCATION`, но помогает точнее объяснить назначение разрешений в тех сценариях, где location действительно не используется.

### Ограничения частоты сканирования

Wi-Fi scanning throttling является одним из главных ограничений.

| Версия Android | Ограничение |
|---|---|
| Android 8.0/8.1 | Каждое фоновое приложение может сканировать один раз за 30 минут |
| Android 9 | Foreground-приложение может выполнить 4 скана за 2 минуты; все background-приложения вместе - 1 скан за 30 минут |
| Android 10+ | Применяются ограничения Android 9; для локального тестирования есть Developer option `Wi-Fi scan throttling` |

Следствие для архитектуры: приложение не должно делать tight loop со `startScan()`. Нужно хранить последний успешный scan cache, показывать возраст данных, использовать passive scan broadcasts и включать активное сканирование только когда пользователь явно открыл экран мониторинга или запустил foreground-сессию.

### Wi-Fi RTT

Wi-Fi RTT позволяет измерять расстояние до RTT-capable Wi-Fi AP или Wi-Fi Aware peer без подключения к этой точке доступа. При трех и более AP можно применять multilateration и получать indoor-позиционирование, обычно в пределах 1-2 метров по описанию Android Developers. Требования жесткие: Android 9+, поддержка 802.11-2016 FTM или 802.11az, включенные Location services и Wi-Fi scanning, совместимые AP, а запросы должны выполняться пока приложение видимо или работает foreground service. На Android 13+ для `WifiRttManager.startRanging()` нужен `NEARBY_WIFI_DEVICES`, на более старых версиях - `ACCESS_FINE_LOCATION`.

RTT стоит рассматривать как отдельный модуль. Он не заменяет Wi-Fi scanning, потому что большинство бытовых AP может не поддерживать FTM/802.11az.

### Wi-Fi Direct и Wi-Fi Aware

Wi-Fi Direct и Wi-Fi Aware полезны не столько для пассивного мониторинга чужих сетей, сколько для обнаружения peer devices и построения локального взаимодействия между устройствами. Они требуют отдельных API и разрешений, начиная с Android 13 - `NEARBY_WIFI_DEVICES`. Для проекта мониторинга их стоит добавлять, если нужно обнаруживать собственные устройства/датчики или peer-приложения рядом.

### Что нельзя сделать через stock Android Wi-Fi API

Обычное приложение не может:

- перевести встроенный Wi-Fi-чип в monitor mode;
- получить все 802.11 management/control/data frames;
- слушать чужой Wi-Fi traffic вне ассоциации;
- получать raw PHY/MAC данные, CSI/IQ samples или spectrum waterfall;
- надежно опрашивать эфир с высокой частотой в фоне;
- обходить системный Location toggle для scan results.

Для monitor mode на смартфонах обычно требуются кастомные прошивки/модификации firmware Wi-Fi-чипа или внешние адаптеры. Исследование NexMon прямо описывает проблему: смартфоны часто используют FullMAC Wi-Fi-чипы, где monitor mode обычно не реализован в штатной firmware.

### MAC randomization и приватность

Начиная с Android 8 устройства используют randomized MAC при probing для новых сетей, а в Android 10 MAC randomization включена по умолчанию для client mode, SoftAP и Wi-Fi Direct. Это влияет на задачи мониторинга устройств по Wi-Fi probe/request активности: обычное приложение все равно не видит сырые probe frames, а даже специализированный внешний сниффер будет наблюдать randomized identifiers, а не стабильный factory MAC.

## Bluetooth

### Bluetooth Classic

Для Bluetooth Classic Android позволяет получить список уже paired devices через `BluetoothAdapter.getBondedDevices()` и запустить discovery через `startDiscovery()`. Discovery находит только те устройства, которые отвечают на inquiry, то есть находятся в discoverable-состоянии. Android-устройства по умолчанию не discoverable; пользователь или приложение должны явно включить discoverability на ограниченное время.

Для мониторинга это означает, что Classic discovery не даст полный список всех Bluetooth-устройств рядом. Он подходит для поиска аксессуаров, pairing flow и диагностики уже известных устройств, но не для непрерывного пассивного радара.

### BLE

BLE scanning - основной практичный путь для мониторинга Bluetooth-окружения. Используется `BluetoothLeScanner.startScan()`, результаты приходят в `ScanCallback`. В `ScanResult` обычно полезны:

- `device` и его address/name, если доступны;
- `rssi`;
- `scanRecord`, включая service UUID, manufacturer data, service data;
- advertising flags, TX power, connectable/scannable признаки, PHY, periodic advertising данные, если поддерживается.

BLE хорошо подходит для поиска датчиков, маяков, wearable-устройств и собственных периферийных устройств проекта. Для экономии батареи Android Developers рекомендуют прекращать scan сразу после нахождения нужного устройства, не сканировать в бесконечном цикле и всегда задавать лимит времени.

### Разрешения Bluetooth

Для target Android 12+ используются runtime permissions группы Nearby devices:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- Только если scan results используются для вывода физической локации. -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Если приложение не выводит физическую локацию из Bluetooth scan results, можно указать:

```xml
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
```

Но у этого есть цена: Android предупреждает, что некоторые BLE beacons могут быть отфильтрованы из результатов. Для приложения, которое именно мониторит BLE-маяки, `neverForLocation` может ухудшить полноту данных. Решение зависит от продуктового смысла: если нужны iBeacon/Eddystone-like сценарии и indoor proximity, лучше явно объяснять location usage и запрашивать location там, где это требуется политиками и API.

Для Android 11 и ниже Bluetooth scan мог требовать `ACCESS_FINE_LOCATION`, потому что результаты Bluetooth-сканирования могут использоваться для определения местоположения пользователя.

### Фоновая работа BLE

Для фонового BLE-мониторинга Android рекомендует не будить процесс периодическим polling-сканом. Более правильные варианты:

- `BluetoothLeScanner.startScan()` с `PendingIntent`, чтобы система разбудила приложение при совпадении фильтра;
- `CompanionDeviceManager` / `CompanionDeviceService` для связанных companion-устройств;
- `WorkManager` для коротких фоновых задач;
- foreground service с типом `connectedDevice`, если нужно удерживать соединение или слушать notifications.

Фоновое подключение к BLE-устройству само по себе не запрещено, но процесс может быть убит системой, а запуск activities и foreground services из background ограничен на новых Android версиях.

### Что нельзя сделать через stock Bluetooth API

Обычное приложение не может:

- получить все Bluetooth Classic/BLE link-layer packets;
- сниффить чужое соединение;
- читать encrypted traffic между двумя другими устройствами;
- гарантированно видеть все BLE advertising packets в эфире;
- стабильно идентифицировать BLE-устройство, если оно использует rotating private address;
- одновременно сканировать BLE и Classic одним и тем же API-процессом discovery как единый поток: Android документация прямо указывает, что нельзя сканировать BLE и Classic одновременно.

Для packet-level анализа нужны внешние снифферы, например специализированный BLE sniffer, nRF Sniffer, Ellisys, Ubertooth-like hardware или собственное устройство-посредник.

## Сотовые и другие радиосигналы

### Сотовая сеть через TelephonyManager

Android позволяет получать часть информации о сотовом радиомодуле, но это не raw RF. Практичные API:

- `TelephonyManager.requestCellInfoUpdate()` - сведения о serving и neighboring cells;
- `TelephonyCallback.CellInfoListener` - обновления `CellInfo`;
- `TelephonyCallback.SignalStrengthsListener` - обновления `SignalStrength`;
- `SignalStrength.getCellSignalStrengths()` - granular signal strength по LTE/NR/GSM/WCDMA/CDMA типам;
- `CellInfo.getCellIdentity()` и `getCellSignalStrength()` - identity и signal metrics конкретной соты, если доступны.

`requestCellInfoUpdate()` требует `ACCESS_FINE_LOCATION` и feature `FEATURE_TELEPHONY_RADIO_ACCESS`. Это логично: cell identity и neighboring cells позволяют приблизительно определить местоположение устройства.

Более активный `requestNetworkScan()` сильно ограничен. Для обычных приложений он фактически недоступен, потому что требует `MODIFY_PHONE_STATE` или carrier privileges, плюс location. Поэтому приложение из Google Play не должно проектироваться вокруг произвольного сканирования cellular bands/channels.

### UWB, NFC, GNSS

UWB и NFC - это не общий радиосканер, но могут быть частью мониторинга nearby devices:

- UWB дает ranging для совместимых устройств и требует поддержки железа/разрешений Nearby devices.
- NFC работает только на очень малых расстояниях и в пользовательском сценарии поднесения метки/карты.
- GNSS API дают местоположение и часть статуса спутников, но не дают произвольный RF spectrum или IQ samples с антенны.

### Внешний SDR или специализированный приемник

Если цель - мониторить радиосигналы шире Wi-Fi/Bluetooth/cellular metadata, нужен внешний приемник. Android может работать в USB host mode: устройство выступает USB host, питает шину, enumerates USB devices и может общаться с ними через `UsbManager`, `UsbDevice`, `UsbInterface`, `UsbEndpoint`, `UsbDeviceConnection`. Для SDR это означает, что приложение может быть клиентом к RTL-SDR/HackRF/другому устройству, если есть совместимые native libraries, USB permission flow и достаточная производительность.

Варианты архитектуры:

- Android-приложение + USB SDR через OTG: максимум контроля, но больше проблем с питанием, драйверами, native кодом и Play distribution.
- Android-приложение + внешний датчик по BLE/Wi-Fi: проще для массового продукта, датчик сам выполняет RF capture/processing, приложение получает агрегированные события.
- Android-приложение как UI к серверу/edge gateway: SDR работает на Linux/embedded device, Android получает результаты по сети.

Для проекта, ориентированного на Android-разработку, самый устойчивый путь - вынести raw RF processing во внешний модуль, а на Android оставить permissions, UX, локальное хранение, визуализацию и синхронизацию.

## Ограничения платформы и политики

### Privacy model

Wi-Fi/Bluetooth/cell scan results считаются чувствительными, потому что по ним можно выводить местоположение и перемещение пользователя. Отсюда вытекают требования к location permission, Nearby devices permission, включенному системному Location toggle, throttling и фоновым ограничениям.

Google Play отдельно проверяет background location. Если мониторинг работает, когда приложение не видно пользователю, нужно доказать, что это core functionality, дать prominent disclosure, privacy policy, заполнить permissions declaration и показать feature в видео. Если можно реализовать сценарий через foreground-сессию, это обычно проще и надежнее.

### Battery model

Активное сканирование дорогое. Постоянные Wi-Fi scans и BLE scans быстро расходуют батарею и могут быть ограничены системой. Для мониторинга нужно проектировать sampling policy:

- foreground screen: быстрый интервал, например 5-15 секунд для BLE и по возможности не чаще разрешенного Wi-Fi throttling;
- foreground service: умеренный интервал и явное notification;
- background: event-driven через PendingIntent/CDM/passive broadcasts, без постоянного polling;
- adaptive backoff: если окружение стабильно, снижать частоту;
- показывать возраст данных в UI.

### OEM fragmentation

Поведение сканирования отличается между устройствами: Wi-Fi/BLE-чипы, vendor firmware, энергосбережение, агрессивные background restrictions, поддержка RTT/UWB/5G APIs, доступность конкретных полей `ScanResult`. Для проекта нужно закладывать capability detection и telemetry ошибок, а не считать API одинаковым на всех телефонах.

## Рекомендуемая архитектура для Android-приложения

### Модули

| Модуль | Ответственность |
|---|---|
| `PermissionCoordinator` | Проверка runtime permissions, объяснение пользователю, маршрутизация на settings, обработка denied/permanently denied |
| `WirelessCapabilityProvider` | Проверка feature flags: Wi-Fi, BLE, Bluetooth Classic, Telephony, RTT, UWB, USB host |
| `WifiMonitor` | Passive scan receiver, controlled `startScan()`, нормализация `ScanResult` |
| `BleMonitor` | BLE scan sessions, filters, callbacks/PendingIntent mode, нормализация advertising data |
| `BluetoothClassicMonitor` | Bonded devices и short discovery sessions |
| `CellularMonitor` | `TelephonyCallback`, `requestCellInfoUpdate()`, нормализация `CellInfo`/`SignalStrength` |
| `ExternalRadioMonitor` | USB/BLE/Wi-Fi bridge к внешнему SDR или RF sensor |
| `SamplingPolicy` | Foreground/background intervals, throttling awareness, backoff |
| `ObservationRepository` | Room/SQL хранение наблюдений и агрегатов |
| `Export/Sync` | CSV/JSON/export в backend, если нужен аудит |

### Модель данных

Базовая сущность может быть общей:

```kotlin
data class RadioObservation(
    val id: String,
    val source: Source, // WIFI, BLE, BT_CLASSIC, CELLULAR, UWB, EXTERNAL_RF
    val observedAtMillis: Long,
    val stableKey: String?,
    val displayName: String?,
    val rssiDbm: Int?,
    val frequencyMhz: Int?,
    val channel: Int?,
    val metadata: Map<String, String>,
    val rawPayload: ByteArray? = null
)
```

`stableKey` должен быть осторожным: BSSID для Wi-Fi AP обычно стабилен, но BLE address может быть randomized, а cell identity чувствительна к privacy. Для пользовательского продукта лучше хранить хэши идентификаторов, если не нужен сырой ID.

### UX

Рекомендуемый UX:

- главный экран показывает текущую foreground-сессию мониторинга;
- отдельные вкладки: Wi-Fi, BLE, Bluetooth Classic, Cellular, External RF;
- в каждой строке явно показывать `last seen`, RSSI, источник, confidence/age;
- отдельный экран Permissions & Diagnostics с объяснением, почему нужны Location/Nearby devices/Bluetooth/USB;
- для фонового мониторинга - явный toggle, foreground notification и описание, какие данные собираются;
- показывать предупреждение, если Location toggle выключен, Wi-Fi/Bluetooth выключены или throttling не дает свежих данных.

## Практические сценарии

### Реалистично в первой версии

- Список видимых Wi-Fi сетей с RSSI, BSSID, частотой, security capabilities и временем последнего обновления.
- BLE scanner с фильтрами по service UUID/manufacturer data и графиком RSSI.
- Список paired Bluetooth devices и короткий Classic discovery по кнопке.
- Сотовый статус: текущая технология, уровень сигнала, serving cell, neighboring cells там, где API/оператор/устройство дают данные.
- Локальная история наблюдений и экспорт CSV/JSON.
- Foreground monitoring session с notification.

### Реалистично как расширение

- Wi-Fi RTT ranging для совместимых AP.
- UWB ranging для совместимых устройств.
- CompanionDeviceManager flow для собственных BLE-датчиков.
- USB host integration с внешним RF sensor/SDR.
- Backend-корреляция данных от нескольких Android устройств.

### Нереалистично без root/железа

- Wi-Fi packet sniffer/monitor mode на встроенном чипе.
- Bluetooth sniffer чужих соединений.
- Spectrum analyzer всех радиочастот с телефона.
- Обход scan throttling и location toggle.
- Надежная уникальная идентификация всех окружающих телефонов по Wi-Fi/BLE.

## Риски для проекта

| Риск | Влияние | Митигация |
|---|---|---|
| Permission denial | Сканирование не работает или дает пустые данные | Permissions screen, graceful degradation, диагностика причин |
| Location toggle выключен | Wi-Fi/cellular scan APIs падают или пустые | Проверять настройки и показывать actionable state |
| Wi-Fi throttling | Данные устаревают | Passive scans, age indicator, foreground-only активный scan |
| BLE random addresses | Нельзя стабильно трекать устройство | Использовать service/manufacturer payload, bonding, собственные идентификаторы в payload |
| OEM background restrictions | Фоновая работа нестабильна | Foreground service, CDM, PendingIntent BLE scan, тестовая матрица устройств |
| Play review | Релиз может быть отклонен | Минимальные permissions, disclosure, privacy policy, foreground-first design |
| Неверная трактовка RSSI | Плохая оценка расстояния | Не обещать точную дистанцию; использовать RTT/UWB для ranging |

## Тестовая матрица

Минимально стоит протестировать:

- Android 10, 11, 12, 13, 14, 15+;
- устройства Samsung, Pixel, Xiaomi/Redmi, OnePlus/OPPO, если целевая аудитория широкая;
- сценарии: foreground screen, foreground service, app background, screen off, battery saver, denied permissions, Location off, Bluetooth off, Wi-Fi off;
- Wi-Fi: throttling, passive broadcasts, старые scan results, разные security capabilities;
- BLE: low latency scan, balanced scan, фильтры, PendingIntent scan, random/private addresses;
- Telephony: single SIM, dual SIM, no SIM, airplane mode, 4G/5G, отсутствие `FEATURE_TELEPHONY_RADIO_ACCESS`;
- USB: attach/detach, permission dialog, питание внешнего устройства, поворот экрана, process death.

## Итоговая рекомендация

Для Android-приложения мониторинга стоит формулировать продукт как "мониторинг беспроводного окружения средствами Android API", а не как "радиосканер". Встроенные API дают достаточно данных для Wi-Fi/BLE/cellular observability, но не дают сырой эфир. Если проекту нужен настоящий радиочастотный мониторинг, это должен быть отдельный hardware-backed режим с внешним SDR/датчиком.

Оптимальная первая реализация: foreground-first app, Wi-Fi scan cache + controlled scan, BLE scanner с фильтрами, cellular signal dashboard, единая модель наблюдений, явная диагностика разрешений и честное отображение свежести данных. Это технически реализуемо, лучше проходит политики Android/Google Play и оставляет путь к расширению через RTT/UWB/USB SDR.

## Источники

- Android Developers: [Wi-Fi scanning overview](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- Android Developers: [Request permission to access nearby Wi-Fi devices](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)
- Android Developers: [Wi-Fi location: ranging with RTT](https://developer.android.com/develop/connectivity/wifi/wifi-rtt)
- Android Developers: [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- Android Developers: [Find BLE devices](https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices)
- Android Developers: [Find Bluetooth devices](https://developer.android.com/develop/connectivity/bluetooth/find-bluetooth-devices)
- Android Developers: [BLE: Communicate in the background](https://developer.android.com/develop/connectivity/bluetooth/ble/background)
- Android Developers API Reference: [TelephonyManager](https://developer.android.com/reference/android/telephony/TelephonyManager)
- Android Developers API Reference: [SignalStrength](https://developer.android.com/reference/android/telephony/SignalStrength)
- Android Developers API Reference: [CellInfo](https://developer.android.com/reference/android/telephony/CellInfo)
- Android Developers: [USB host overview](https://developer.android.com/develop/connectivity/usb/host)
- Android Open Source Project: [Implement MAC randomization](https://source.android.com/docs/core/connect/wifi-mac-randomization)
- Google Play Console Help: [Understanding location in the background permissions](https://support.google.com/googleplay/android-developer/answer/9799150)
- Schulz, Wegemer, Hollick: [NexMon: A Cookbook for Firmware Modifications on Smartphones to Enable Monitor Mode](https://arxiv.org/abs/1601.07077)
