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
│   └── sample/                          # ВРЕМЕННО: примеры файлов в filesDir/storage
└── src/main/AndroidManifest.xml
```

* **Слой:** Presentation.
* **Ключевые технологии:** Jetpack Compose, Navigation Compose, Hilt.
* **Зависимости:** `:core:common-ui`, `:core:document:impl`, `:core:logger:client:api` (через фичи), `:features:file-manager`, `:features:document-viewer`, `:features:nearby-scanner`, `:features:condition-data-fetcher`.

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

Пакет `sample` при первом запуске кладёт в `filesDir/storage` (корень файлового менеджера) примеры для файлового менеджера и просмотрщика: docx с таблицей, картинками и диаграммами, csv, md, pdf, png, wav, тексты в UTF-8 и cp1251. Набор записывается один раз на версию (SharedPreferences `sample_files`). Перед релизом удалить пакет `sample` и его вызов в `MonitoringDevicesApplication`.

## Подключение модуля к проекту

`:app` — конечное приложение, его не подключают к другим модулям. Он сам подключает фичи и реализацию чтения документов:

```kotlin
// build.gradle.kts приложения
implementation(project(":features:file-manager"))
implementation(project(":features:document-viewer"))
implementation(project(":core:document:impl"))
```

Фичам нужен Hilt: `@HiltAndroidApp` на `MonitoringDevicesApplication`, `@AndroidEntryPoint` на `MainActivity`, а `DocumentReader` для просмотрщика даёт `di/DocumentCoreModule.kt`.

Сборка:

```bash
./gradlew :app:assembleDebug
```

## Описание API

Модуль — конечное приложение, своего API у него нет.

## Примеры использования

Новый экран фичи добавляется в `navigation/AppNavigation.kt` маршрутом и кнопкой на `HomeScreen`. Так подключены файловый менеджер и просмотрщик: менеджер отдаёт выбранный файл, приложение открывает по нему просмотрщик.

```kotlin
composable(AppDestination.FILE_MANAGER) {
    fileManagerFeature.FileManagerScreen(
        modifier = Modifier,
        onBackClick = { navController.popBackStack() },
        onFileOpen = { file -> navController.navigate(AppDestination.fileViewer(file)) }
    )
}
composable(
    route = AppDestination.FILE_VIEWER,
    arguments = listOf(navArgument(AppDestination.ARG_PATH) { type = NavType.StringType })
) { backStackEntry ->
    documentViewerFeature.DocumentViewerScreen(
        file = File(backStackEntry.arguments?.getString(AppDestination.ARG_PATH).orEmpty()),
        modifier = Modifier,
        onBackClick = { navController.popBackStack() }
    )
}
```
