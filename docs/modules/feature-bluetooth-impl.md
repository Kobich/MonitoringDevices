# Модуль `:feature:bluetooth:impl`

Модуль реализует Bluetooth экран: управляет monitoring session, разрешениями, сортировкой, визуальным устареванием устройств и просмотром деталей. Android Bluetooth API вызывается только scanner-слоем.

## Зависимости и поток данных

Модуль реализует `feature:bluetooth:api` и зависит от `service:monitoring:api` и `scanner:bluetooth:api`.

```text
BluetoothFeatureImpl
  -> BluetoothFeatureScreen
  -> BluetoothViewModel
  -> BluetoothScanInteractor
  -> MonitoringController
  -> BluetoothScanResult
  -> BluetoothUiStateReducer
  -> BluetoothScreen
```

`BluetoothScanInteractor` переводит инфраструктурные `BluetoothScanEvent` во внутренние `BluetoothScanResult`. ViewModel автоматически подключается к уже активному monitoring и не останавливает его при уничтожении экрана. Явный Stop удаляет Bluetooth из active types.

## Файлы и классы

| Файл | Назначение |
|---|---|
| `BluetoothFeatureImpl.kt` | Реализация `BluetoothFeatureApi`. |
| `BluetoothFeatureModule.kt` | Koin bindings для API, interactor, mapper, reducer и ViewModel. |
| `BluetoothScanInteractor.kt` | Запускает/останавливает Bluetooth monitoring и преобразует поток controller. |
| `BluetoothScanResult.kt` | Внутренняя sealed-модель результатов фичи. |
| `BluetoothFeatureScreen.kt` | Связывает ViewModel, permission launcher и stateless screen. |
| `BluetoothViewModel.kt` | Обрабатывает Start, Stop, permissions, сортировку и выбор устройства. |
| `BluetoothUiState.kt` | UI state, режимы сортировки, модель устройства и permission request. |
| `BluetoothDeviceUiMapper.kt` | Преобразует scanner model в UI model и применяет выбранную сортировку. |
| `BluetoothUiStateReducer.kt` | Обновляет state по результатам scan и сохраняет выбранное устройство. |
| `BluetoothScreen.kt` | Основная Compose-разметка, статусы, сортировка, кнопки и список. |
| `BluetoothDeviceCard.kt` | Карточка устройства; уменьшает прозрачность устаревшей BLE-записи. |
| `BluetoothDeviceDetailsSheet.kt` | Показывает имя, адрес, RSSI, тип и bond state. |

## Сортировка и aging

Доступны три режима:

- `STABLE` сохраняет текущий порядок существующих адресов и добавляет новые устройства в конец;
- `SIGNAL` сортирует по убыванию RSSI;
- `NAME` сортирует по имени, затем по адресу, помещая неизвестные имена после известных.

BLE-запись считается устаревшей после 30 секунд без нового пакета и становится полупрозрачной. Удаление после 120 секунд выполняет scanner. Classic-записи сейчас не имеют возраста.

## Ограничения

Адрес используется как ключ строки и идентификатор стабильной сортировки, но BLE-устройства могут менять private address. Screen-off suspension, режим `ALL`, стоимость частых snapshots и OEM restrictions описаны в [ограничениях платформы](../platform-limitations.md).
