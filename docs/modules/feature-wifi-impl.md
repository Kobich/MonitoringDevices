# Модуль `:feature:wifi:impl`

Модуль реализует Wi-Fi экран: запускает и останавливает мониторинг, запрашивает разрешения, показывает статус throttling, список сетей и детали выбранной точки доступа. Фича получает данные через `MonitoringController`; непосредственный Android Wi-Fi API остаётся в `scanner:wifi:impl`.

## Зависимости и поток данных

Модуль реализует `feature:wifi:api` и зависит от `service:monitoring:api` и `scanner:wifi:api`.

```text
WifiFeatureImpl
  -> WifiFeatureScreen
  -> WifiViewModel
  -> WifiScanInteractor
  -> MonitoringController
  -> WifiScanEvent
  -> WifiUiState.reduce()
  -> WifiScreen
```

ViewModel автоматически подключается к уже активному Wi-Fi monitoring. Кнопка Stop удаляет только Wi-Fi тип из monitoring state. Изменение системного throttling проверяется при `ON_RESUME`; если scan уже активен, UI collection перезапускается с актуальным режимом.

## Файлы и классы

| Файл | Назначение |
|---|---|
| `WifiFeatureImpl.kt` | Реализация `WifiFeatureApi`, открывающая внутренний route. |
| `WifiFeatureModule.kt` | Koin bindings для API, interactor и ViewModel. |
| `WifiScanInteractor.kt` | Запускает Wi-Fi monitoring, подписывается на события controller и останавливает выбранный тип. |
| `WifiFeatureScreen.kt` | Связывает ViewModel, lifecycle, permission launcher и stateless screen; открывает Developer options. |
| `WifiViewModel.kt` | Владеет UI job и `WifiUiState`, обрабатывает Start, Stop, permissions, throttling и выбор сети. |
| `WifiUiState.kt` | Модели состояния экрана, строки сети и permission request. |
| `WifiUiStateReducer.kt` | Преобразует `WifiScanEvent` в UI state, сортирует сети по RSSI и синхронизирует выбранную сеть. |
| `WifiScreen.kt` | Основная Compose-разметка, статусы, кнопки, список, throttling dialog и bottom sheet. |
| `WifiNetworkCard.kt` | Карточка сети с SSID, BSSID, RSSI и частотой. |
| `WifiNetworkDetailsSheet.kt` | Детали выбранной сети. |

## Состояние экрана

`WifiUiState` хранит текст статуса, текущий список сетей, выбранную сеть, видимость throttling dialog, признак активного UI collection и запрос разрешений. SSID без имени отображается как `Hidden network`; стабильным ключом строки служит BSSID.

## Ограничения

Фича не определяет фактическую свежесть Wi-Fi cache и не может обойти системный throttling. Поведение scanner и foreground service описано в [ограничениях платформы](../platform-limitations.md).
