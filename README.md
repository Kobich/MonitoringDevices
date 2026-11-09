# MonitoringDevices

Android-проект: фичи для работы с устройством и файлами и приложение-хост для их проверки.

`:core:*` — общий код, `:features:*` — фичи, `:app` — хост. Подробнее — в [docs/architecture.md](docs/architecture.md).

| Модуль | Описание |
|---|---|
| `:core:common-ui` | Тема, значки, общие Compose-компоненты, экран всех значков. |
| `:core:document:api` / `:impl` | Чтение документов: PDF, офисные форматы, текст, картинки, аудио. |
| `:features:file-manager` | Файловый менеджер приватного хранилища: импорт, экспорт, папки. |
| `:features:document-viewer` | Просмотр документов, прочитанных `:core:document`. |
| [`:features:nearby-scanner`](features/nearby-scanner/README.md) | Сканеры Wi-Fi и Bluetooth и их Compose-экраны. |
| [`:features:condition-data-fetcher`](features/condition-data-fetcher/README.md) | Данные об окружении и проверка старт-условий: время, гео, Wi-Fi, Bluetooth. |
| `:app` | Хост: главный экран, навигация, DI. |

Подключение фичи в приложении-хосте:

```kotlin
NavHost(navController = navController, startDestination = "home") {
    composable("home") { HomeScreen(onOpenScannerClick = { navController.navigateToNearbyScan() }) }
    nearbyScanScreen(scanner = NearbyScanner(applicationContext))
}
```

Сборка и тесты:

```bash
./gradlew :app:assembleDebug :features:nearby-scanner:testDebugUnitTest :features:condition-data-fetcher:testDebugUnitTest
```

Подробные заметки по устройству проекта — в [docs/](docs/README.md).
