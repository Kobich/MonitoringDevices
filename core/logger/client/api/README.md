# Логгер действий: затычка клиента

## Описание

Временная замена настоящего клиента логгера с тем же API: `ActionLogger.logUi(event)`. События никуда не отправляются, только пишутся в Logcat. Пакет и имена совпадают с настоящей библиотекой, поэтому её модуль подключается вместо этого без правок в коде вызовов.

## Архитектура и стек

```
core/logger/client/api/
└── src/main/kotlin/com/android/core/logger/client/api/
    ├── ActionLogger.kt         # logUi(event): пишет событие в Logcat
    ├── UiInteractionEvent.kt   # действие, экран, элемент, текст
    └── UiInteractionAction.kt  # Click
```

* **Слой:** Infrastructure, затычка.
* **Ключевые технологии:** `android.util.Log`.
* **Зависимости:** не зависит от других модулей проекта.

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

Логи: `adb logcat -s ActionLogger`.

## Подключение модуля к проекту

```kotlin
// settings.gradle.kts
include(":core:logger:client:api")

// build.gradle.kts модуля, который логирует действия
implementation(project(":core:logger:client:api"))
```

Замена на настоящий клиент: удалить этот модуль из `settings.gradle.kts` и подключить настоящий под тем же путём (или поменять путь в `implementation(...)` у модулей, которые логируют).

## Описание API

| Тип | Что даёт |
|---|---|
| `ActionLogger.logUi(event: UiInteractionEvent)` | Логирует действие пользователя в интерфейсе |
| `UiInteractionEvent(action, screen, elementId, elementType, text = null)` | Событие: что сделали, на каком экране, с каким элементом |
| `UiInteractionAction` | `Click` |

## Примеры использования

```kotlin
ActionLogger.logUi(
    event = UiInteractionEvent(
        action = UiInteractionAction.Click,
        screen = SCREEN_NAME,
        elementId = "audio_start",
        elementType = "button",
        text = "Start"
    )
)
```
