# Ограничения Android и текущей реализации

Страница фиксирует ожидаемое поведение текущего приложения. Она нужна для разработки, тестирования и разбора ситуаций, когда scanner остаётся запущенным на уровне приложения, но Android перестаёт отдавать новые данные.

## Bluetooth LE при выключенном экране

Текущая реализация вызывает `BluetoothLeScanner.startScan(callback)` без `ScanFilter`. Android считает такой scan нефильтрованным, при выключении экрана переводит его в suspended и возобновляет после включения экрана. Разблокировка телефона не требуется: достаточно состояния Display On.

Foreground service сохраняет процесс и зарегистрированный callback, но не отменяет это ограничение Bluetooth stack. Пока экран выключен, новые BLE-пакеты не поступают, aging продолжает увеличивать возраст записей: после `staleAfterMillis` (по умолчанию 30 секунд) устройство помечается `isStale`, после `retentionMillis` (120 секунд) удаляется.

Продолжить BLE-сканирование с выключенным экраном можно только с настоящими непустыми фильтрами по адресу, имени, service UUID или manufacturer data. Пустой `ScanFilter` не считается фильтром. Для универсального scanner, который должен видеть все устройства, подходящего широкого фильтра нет.

## Нагрузка BLE scanner

Каждый BLE advertising result немедленно обновляет внутреннюю map, но полный snapshot отправляется не чаще одного раза в `snapshotIntervalMillis` (750 мс). RSSI сглаживается, а изменения меньше `minRssiChangeDbm` (3 dBm) не обновляют отображаемое значение. Это ограничивает частоту Flow/UI-обновлений, но стоимость одного snapshot всё равно растёт вместе с числом найденных устройств.

Непрерывный нефильтрованный scan также расходует радио и батарею. Android может понижать режим долгого scan, применять квоты к частым повторным запускам и ограничивать приложение по политике энергопотребления. Foreground service делает работу видимой пользователю, но не даёт исключения из Bluetooth-квот.

## Временные OEM-ограничения

Производители могут добавлять собственные правила поверх Android. На Blackview/DokeOS наблюдался отказ запуска `DeviceMonitoringService` с причиной `thirdAppBlacklisted`, состояниями `appidle`, `calledAppFreezeOridle=true` и `callerVisible=false`.

Это не blacklist Bluetooth-устройства. DokeOS временно ограничивает пакет приложения и блокирует запуск service. Ограничение может исчезнуть после паузы и следующего запуска приложения. Вероятность такого поведения повышают продолжительный scan, высокая частота обработки результатов, остановка последнего foreground service и агрессивные настройки энергосбережения.

Для тестирования Blackview необходимо отдельно проверять режим батареи «Без ограничений», разрешение фоновой активности, DuraSpeed/System Manager и повторный запуск после остановки всех scan jobs. Даже при правильной реализации Android API OEM не гарантирует одинаковое поведение.

## Wi-Fi

Активный Wi-Fi scan зависит от включённых Wi-Fi и Location Services, runtime permissions и scan throttling. `WifiManager.startScan()` может вернуть `false`, а broadcast может содержать старый cache. Приложение в таком случае читает доступные cached results и не должно считать каждый event новым физическим сканированием.

При включённом throttling сервис запрашивает scan раз в `throttledRefreshIntervalMillis` (30 секунд), при выключенном — раз в `unthrottledRefreshIntervalMillis` (5 секунд). Статус throttling перечитывается перед каждым ожиданием. Эти интервалы являются политикой приложения, но Android всё равно может отклонить запрос. Фоновое поведение и частота обновления отличаются между версиями Android и прошивками производителей.

## Foreground service и процесс

Foreground service повышает вероятность сохранения процесса, но не гарантирует непрерывную работу. Пользовательский force stop, OEM freezer, revoked permissions и системные ограничения могут остановить service. `START_STICKY` разрешает Android пересоздать service, но не обходит запрет фонового запуска и не восстанавливает потерянные результаты за время остановки процесса.

После остановки последнего типа service снимает foreground-состояние и вызывает `stopSelf()`. Фактическое уничтожение service асинхронно. На агрессивных OEM повторный запуск сразу после остановки следует тестировать отдельно.

## Разрешения

- Wi-Fi требует Wi-Fi state permissions, location permissions и на Android 13+ `NEARBY_WIFI_DEVICES`.
- BLE на Android 12+ требует `BLUETOOTH_SCAN` и `BLUETOOTH_CONNECT`; на старых версиях используются Bluetooth и location permissions.
- Wi-Fi/BLE monitoring на Android 13+ дополнительно требует `POST_NOTIFICATIONS` для foreground service.

Наличие runtime permission не гарантирует доступность данных: системный Location toggle, состояние адаптера, AppOps и политика OEM проверяются отдельно.

## Ожидаемое поведение

| Сценарий | Ожидание |
|---|---|
| Переход между вкладками | Активные Wi-Fi/Bluetooth jobs продолжают работу |
| Сворачивание приложения | Foreground service остаётся активным, если OEM его не ограничил |
| Выключение экрана | Нефильтрованный BLE scan приостанавливается |
| Включение экрана без разблокировки | BLE scan автоматически возобновляется |
| Остановка одного типа | Второй активный тип продолжает работать |
| Остановка последнего типа | Notification удаляется, service завершается |
| Убийство процесса | Непрерывность не гарантируется; возможен системный restart service |
| Частые повторные BLE-старты | Возможен `SCAN_FAILED_SCANNING_TOO_FREQUENTLY` |
| Blackview после высокой нагрузки | Возможен временный `thirdAppBlacklisted` |

## Ссылки

- [BluetoothLeScanner](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner)
- [Поиск BLE-устройств](https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices)
- [BLE в фоне](https://developer.android.com/develop/connectivity/bluetooth/ble/background)
- [Wi-Fi scanning](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- [Ограничения запуска foreground service](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Background optimization](https://developer.android.com/topic/performance/background-optimization)
