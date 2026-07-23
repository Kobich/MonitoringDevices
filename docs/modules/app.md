# Модуль `:app`

`app` — Android application module и единственная точка сборки приложения. Он запускает Koin, подключает реализации feature/scanner/service, применяет Compose-тему и показывает нижнюю навигацию между Wi-Fi, Bluetooth и Radio.

## Зависимости

Модуль зависит от `feature:*:impl`, `scanner:*:impl` и `service:monitoring:impl`. Прямое подключение impl-модулей нужно для сборки DI-графа; UI обращается к фичам только через `FeatureApi`.

## Поток запуска

`MonitoringDevicesApplication` создаёт Koin container и регистрирует все модули. `MainActivity` получает три `FeatureApi`, создаёт Compose content и передаёт управление выбранной фиче. Текущая вкладка хранится через `rememberSaveable`, отдельный navigation framework пока не используется.

## Файлы

| Файл | Назначение |
|---|---|
| `MonitoringDevicesApplication.kt` | Инициализирует Koin и собирает scanner, monitoring и feature-модули. |
| `MainActivity.kt` | Корневая Activity, Compose content и нижняя навигация. |
| `Color.kt` | Цветовые значения темы. |
| `Theme.kt` | `MonitoringDevicesTheme` и выбор светлой/тёмной схемы. |
| `Type.kt` | Typography приложения. |
| `AndroidManifest.xml` | Объявляет application class и launcher Activity. |

## Граница модуля

В `app` не должна появляться логика сканирования или преобразования результатов. Новая пользовательская фича подключается через её `api/impl`, Koin module и новую вкладку либо отдельную навигацию.
