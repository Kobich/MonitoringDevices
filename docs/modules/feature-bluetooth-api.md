# Модуль `:feature:bluetooth:api`

Модуль публикует внешний UI-контракт Bluetooth фичи и скрывает её ViewModel, модели сортировки и scanner-события от `app`.

## Контракт

`BluetoothFeatureApi` предоставляет composable-функцию `BluetoothScreen(modifier)`. Реализация поставляется через Koin из `feature:bluetooth:impl`.

## Файлы

| Файл | Назначение |
|---|---|
| `BluetoothFeatureApi.kt` | Публичный Compose-контракт Bluetooth экрана. |

Контракт остаётся минимальным, пока фича полностью владеет своим пользовательским состоянием.
