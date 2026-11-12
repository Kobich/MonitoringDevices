# MonitoringDevices

Android-проект: фичи для работы с устройством и файлами и приложение-хост для их проверки.

`:core:*` — общий код, `:features:*` — фичи, `:app` — хост. У каждого модуля свой `README.md` по шаблону команды:
описание, архитектура и стек, подключение, API и примеры.

| Модуль | Описание |
|---|---|
| [`:core:common-ui`](core/common-ui/README.md) | Тема, значки, общие Compose-компоненты и окна, витрина UI. |
| [`:core:document:api`](core/document/api/README.md) | Контракт чтения документов: PDF, офисные форматы, текст, картинки, аудио, видео. |
| [`:core:document:impl`](core/document/impl/README.md) | Его реализация: OpenDocument.core и системные API Android. |
| [`:core:logger:client:api`](core/logger/client/api/README.md) | Затычка клиента логгера действий: тот же API, события только в Logcat. |
| [`:features:file-manager`](features/file-manager/README.md) | Файловый менеджер приватного хранилища: импорт, экспорт, папки. |
| [`:features:document-viewer`](features/document-viewer/README.md) | Просмотр документов, прочитанных `:core:document`. |
| [`:features:nearby-scanner`](features/nearby-scanner/README.md) | Сканеры Wi-Fi и Bluetooth и их Compose-экраны. |
| [`:features:condition-data-fetcher`](features/condition-data-fetcher/README.md) | Данные об окружении и проверка старт-условий: время, гео, Wi-Fi, Bluetooth. |
| [`:app`](app/README.md) | Хост: главный экран, навигация, DI. |

Имя модуля через дефис совпадает с последней частью пакета без дефисов: `:features:document-viewer` —
`com.android.appmanager.feature.documentviewer`, `:core:common-ui` — `com.android.core.commonui`.
Фича — один модуль: публичный `*FeatureApi`, остальное `internal`. На `:api` / `:impl` делится только
модуль, от контракта которого зависят другие модули без реализации, как `:core:document`.

Подключение сканера в приложении-хосте:

```kotlin
NavHost(navController = navController, startDestination = "home") {
    composable("home") { HomeScreen(onOpenScannerClick = { navController.navigateToNearbyScan() }) }
    nearbyScanScreen(scanner = NearbyScanner(applicationContext))
}
```

Сборка и тесты:

```bash
./gradlew :app:assembleDebug \
    :core:document:impl:testDebugUnitTest :features:file-manager:testDebugUnitTest \
    :features:nearby-scanner:testDebugUnitTest :features:condition-data-fetcher:testDebugUnitTest
```

Заметки и исследования по сканеру Wi-Fi и Bluetooth — в [docs/](docs/README.md).
