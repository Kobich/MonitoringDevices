# Приложение-хост

## Описание

Приложение для ручной проверки модулей на устройстве: главный экран с кнопками открывает каждую фичу и витрину UI. Модули ничего не знают друг о друге: приложение связывает их навигацией и графом Hilt.

## Архитектура и стек

```
app/
├── src/main/java/com/engboost/monitoringdevices/
│   ├── MonitoringDevicesApplication.kt  # @HiltAndroidApp, создаёт NearbyScanner, кладёт примеры файлов
│   ├── MainActivity.kt                  # @AndroidEntryPoint, тема и навигация
│   ├── navigation/AppNavigation.kt      # NavHost: главный экран и экраны модулей
│   ├── home/HomeScreen.kt               # кнопки перехода
│   ├── di/DocumentCoreModule.kt         # DocumentReader из :core:document:impl как @Singleton
│   ├── featuretest/FeatureTestScreen.kt # ручной вызов каждой фичи condition-data-fetcher
│   └── sample/                          # ВРЕМЕННО: примеры файлов в filesDir
└── src/main/AndroidManifest.xml
```

* **Слой:** Presentation.
* **Ключевые технологии:** Jetpack Compose, Navigation Compose, Hilt.
* **Зависимости:** `:core:common-ui`, `:core:document:impl`, `:features:file-manager`, `:features:document-viewer`, `:features:nearby-scanner`, `:features:condition-data-fetcher`.

Минимальная версия SDK: 36 — её требуют `:features:nearby-scanner` и `:features:condition-data-fetcher`. Версия компиляции SDK: 36.

### Экраны

| Кнопка | Экран |
|---|---|
| File manager | `:features:file-manager`; нажатие на файл открывает `:features:document-viewer` |
| Open nearby scanner | `:features:nearby-scanner` |
| Test features | `FeatureTestScreen`: вызов каждой фичи `:features:condition-data-fetcher` |
| UI catalog | `UiCatalogScreen` из `:core:common-ui`: компоненты и значки |
| Dialogs and windows | `DialogShowcaseScreen` из `:core:common-ui`: все виды окон |

### Бэкап

Данные приложения не покидают устройство: `allowBackup="false"` выключает облачный бэкап, а `res/xml/data_extraction_rules.xml` исключает все области и из облака, и из переноса на новый телефон (Android 12+).

### Примеры файлов (ВРЕМЕННО)

Пакет `sample` при первом запуске кладёт в `filesDir` примеры для файлового менеджера и просмотрщика: docx с таблицей, картинками и диаграммами, csv, md, pdf, png, wav, тексты в UTF-8 и cp1251. Набор записывается один раз на версию (SharedPreferences `sample_files`). Перед релизом удалить пакет `sample` и его вызов в `MonitoringDevicesApplication`.

## Описание API

Модуль — конечное приложение, своего API у него нет.

## Сборка

```bash
./gradlew :app:assembleDebug
```
