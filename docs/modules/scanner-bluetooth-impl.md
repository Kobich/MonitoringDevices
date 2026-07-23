# Модуль `:scanner:bluetooth:impl`

Модуль реализует Bluetooth scanner через `BluetoothLeScanner` и `BluetoothAdapter.startDiscovery()`. Он объединяет результаты в один список, нормализует Android-модели и удаляет устаревшие BLE-записи.

## Работа scanner

Scanner проверяет permissions и доступность adapter, отправляет `Scanning`, затем запускает механизмы выбранного режима. Устройства хранятся в `LinkedHashMap` по адресу. Повторное наблюдение заменяет запись с тем же адресом.

BLE callback обновляет сглаженный RSSI и время последнего наблюдения во внутренней map. Snapshot списка отправляется не чаще одного раза в 750 мс. Aging job раз в 5 секунд пересчитывает `lastSeenAgoMillis`; после 120 секунд без пакета BLE-запись удаляется. Classic discovery отправляет найденные устройства через BroadcastReceiver, но не участвует в aging.

## Файлы и классы

| Файл | Назначение |
|---|---|
| `AndroidBluetoothScanner.kt` | Permissions, BLE callback, Classic receiver, snapshot списка, aging и cleanup. |
| `BluetoothScanDiagnostics.kt` | Проверяет наличие adapter, включённый Bluetooth и доступность BLE scanner. |
| `BluetoothScannerModule.kt` | Регистрирует singleton `BluetoothScanner` в Koin. |
| `AndroidManifest.xml` | Объявляет Bluetooth и location permissions для поддерживаемых Android-версий. |

## Lifecycle

Scanner использует `callbackFlow`. При отмене collection он отменяет aging job, вызывает `cancelDiscovery()`, снимает receiver и вызывает `stopScan()` с тем же callback. Activity context не сохраняется.

## Текущие особенности

- BLE scan нефильтрованный и приостанавливается Android при выключенном экране.
- BLE results накапливаются во внутренней map, полный snapshot отправляется не чаще одного раза в 750 мс.
- RSSI сглаживается, изменения меньше 3 dBm не обновляют отображаемое значение.
- Classic discovery запускается один раз и не повторяется после завершения.
- `ALL` одновременно запускает Classic и BLE, что не гарантируется Android.
- Rotating BLE address создаёт новую запись; старая удаляется только aging-механизмом.

Эти пункты подробно разобраны в [ограничениях платформы](../platform-limitations.md).
