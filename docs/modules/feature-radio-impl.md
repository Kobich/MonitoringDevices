# Модуль `:feature:radio:impl`

Модуль реализует короткий пользовательский запрос параметров сотовой сети. В отличие от Wi-Fi и Bluetooth, Radio не использует `service:monitoring`: scan job принадлежит ViewModel и завершается после получения результата.

## Зависимости и поток данных

Модуль реализует `feature:radio:api` и напрямую зависит от `scanner:radio:api`.

```text
DefaultRadioFeature
  -> RadioRoute
  -> RadioViewModel
  -> RadioScanner.scan()
  -> RadioScanEvent
  -> RadioUiState
```

## Файлы и классы

| Файл | Назначение |
|---|---|
| `DefaultRadioFeature.kt` | Реализация `RadioFeatureApi`. |
| `RadioFeatureModule.kt` | Koin bindings для feature API и ViewModel. |
| `RadioRoute.kt` | Permission launcher, основная Compose-разметка и callbacks Start/Stop. |
| `RadioViewModel.kt` | Управляет scan job, разрешениями и отображаемым результатом; содержит `RadioUiState` и `PermissionRequestUi`. |

## Отображаемые данные

Экран показывает общий статус, перечень требуемых разрешений, количество найденных cells и лучший доступный сигнал. Лучшей считается запись с максимальным `signalStrengthDbm`; строка содержит network type, dBm и имя оператора.

## Ограничения

Результат является моментальным чтением `allCellInfo`, а не непрерывным потоком. Данные могут быть cached или отсутствовать. Подробности находятся в [ограничениях платформы](../platform-limitations.md).
