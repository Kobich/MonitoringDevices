# Модуль `:feature:radio:api`

Модуль публикует внешний UI-контракт Radio фичи. В контракт не входят telephony models и разрешения.

## Контракт

`RadioFeatureApi` предоставляет composable-функцию `RadioScreen(modifier)`. `app` получает реализацию через Koin.

## Файлы

| Файл | Назначение |
|---|---|
| `RadioFeatureApi.kt` | Публичный Compose-контракт Radio экрана. |
