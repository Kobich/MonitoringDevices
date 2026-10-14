# Ограничения Android и текущей реализации

Страница фиксирует ожидаемое поведение приложения. Она нужна для разработки, тестирования и разбора ситуаций, когда сканирование запущено, но Android не отдаёт новые данные.

## Сканирование только на открытом экране

Фонового режима нет: foreground service, уведомление и восстановление после пересоздания процесса не используются. Сценарий — открыли приложение, посмотрели окружение, закрыли.

- `DeviceMonitoring.scanWifi()` и `scanBluetooth()` — холодные Flow: сканирование идёт, пока Flow собирается, и останавливается при отмене.
- Тестовые экраны собирают Flow через `repeatOnLifecycle(STARTED)`: при сворачивании приложения или выключении экрана сканирование останавливается, при возврате — возобновляется, если пользователь не нажимал Stop.
- После возобновления Bluetooth-список собирается заново: история advertising-пакетов не сохраняется между запусками скана.

## Нагрузка Bluetooth scanner

Сканирование нефильтрованное: `BluetoothLeScanner.startScan(callback)` без `ScanFilter`. Каждый advertising result немедленно обновляет внутреннюю map, но полный snapshot отправляется не чаще одного раза в `snapshotIntervalMillis` (750 мс). RSSI сглаживается, а изменения меньше `minRssiChangeDbm` (3 dBm) не обновляют отображаемое значение. Это ограничивает частоту Flow/UI-обновлений, но стоимость одного snapshot растёт вместе с числом найденных устройств.

Android применяет квоты к частым повторным запускам BLE scan: если много раз подряд сворачивать и разворачивать приложение, возможен `SCAN_FAILED_SCANNING_TOO_FREQUENTLY` (около 5 запусков за 30 секунд).

## Wi-Fi

Активный Wi-Fi scan зависит от включённых Wi-Fi и Location Services, runtime permissions и scan throttling. `WifiManager.startScan()` может вернуть `false`, а broadcast может содержать старый cache. Приложение в таком случае читает доступные cached results и не должно считать каждый event новым физическим сканированием.

При включённом throttling scan запрашивается раз в `throttledRefreshIntervalMillis` (30 секунд), при выключенном — раз в `unthrottledRefreshIntervalMillis` (5 секунд). Статус throttling перечитывается перед каждым ожиданием. Эти интервалы являются политикой приложения, но Android всё равно может отклонить запрос: с включённым throttling приложению на экране доступно 4 скана за 2 минуты.

## Разрешения

- Wi-Fi требует Wi-Fi state permissions, location permissions и на Android 13+ `NEARBY_WIFI_DEVICES`.
- Bluetooth на Android 12+ требует `BLUETOOTH_SCAN` и `BLUETOOTH_CONNECT`; на старых версиях используются Bluetooth и location permissions.

Наличие runtime permission не гарантирует доступность данных: системный Location toggle, состояние адаптера, AppOps и политика OEM проверяются отдельно.

## Ожидаемое поведение

| Сценарий | Ожидание |
|---|---|
| Переход между вкладками | Скан неактивной вкладки останавливается вместе с её экраном, состояние вкладки сохраняется в ViewModel |
| Сворачивание приложения или выключение экрана | Сканирование останавливается |
| Возврат в приложение | Сканирование возобновляется, если перед уходом оно было запущено |
| Закрытие приложения | Сканирование остановлено, ничего не работает в фоне |
| Частые перезапуски BLE scan | Возможен `SCAN_FAILED_SCANNING_TOO_FREQUENTLY` |

## Ссылки

- [BluetoothLeScanner](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner)
- [Поиск BLE-устройств](https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices)
- [Wi-Fi scanning](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
