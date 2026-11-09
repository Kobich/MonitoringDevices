# Просмотр документов

## Описание

Экран, который показывает файл приложения: PDF, офисные документы, текст, картинки, аудио и видео. Сам файл читает `:core:document`, фича только показывает результат, спрашивает пароль и объясняет ошибку, если файл открыть нельзя.

## Архитектура и стек

```
features/document-viewer/
├── src/main/kotlin/com/android/appmanager/feature/documentviewer/
│   ├── DocumentViewerFeatureApi.kt        # публичный контракт фичи
│   ├── DocumentViewerFeatureImpl.kt       # его реализация, internal
│   ├── di/                                # Hilt-модуль: API фичи
│   ├── data/player/                       # PlayerFactory: Media3 ExoPlayer для аудио и видео
│   └── presentation/
│       ├── DocumentViewerFeatureScreen.kt # Route: ViewModel по File, пауза при уходе в фон
│       ├── DocumentViewerViewModel.kt     # открывает документ, пароль, ошибки; владеет документом и плеерами
│       ├── AudioPlayback.kt               # состояние аудиоплеера: прогресс, play/pause/seek
│       ├── model/                         # UiState: Loading / Loaded(document) / Error
│       └── screen/                        # экран и по экрану на вид документа, диалог пароля
└── src/main/res/values/strings.xml        # строки с префиксом viewer_
```

* **Слой:** Presentation.
* **Ключевые технологии:** Jetpack Compose, Material 3, Hilt (assisted injection: ViewModel создаётся под `File`), Media3 ExoPlayer, WebView + `WebViewAssetLoader`.
* **Зависимости:** `:core:document:api`, `:core:common-ui`. `DocumentReader` фича берёт из графа Hilt приложения: его даёт `:core:document:impl` (см. `DocumentCoreModule` в `:app`).

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

### Как показывается каждый вид

| Документ | Экран |
|---|---|
| `PdfDocument` | Страницы списком, каждая рисуется под ширину экрана, когда становится видна |
| `HtmlDocument` | WebView; у таблиц вкладки по листам. Файлы отдаёт `WebViewAssetLoader`: без сети и доступа к файлам, остальные запросы блокируются |
| `TextDocument` | Текст построчно в ленивом списке, сверху кодировка и признак обрезки |
| `ImageDocument` | Картинка с pinch-zoom и двойным тапом; GIF и WebP анимируются на Android 9+ |
| `AudioDocument` | Обложка, название, плеер с перемоткой |
| `VideoDocument` | Media3 `PlayerView` с кнопками управления |

Плееры держит ViewModel, поэтому воспроизведение переживает поворот экрана. Когда экран уходит в фон, плеер встаёт на паузу. Освобождается он вместе с документом.

Логи: `adb logcat -s DocumentViewer AudioPlayback Player HtmlDocument`.

## Подключение модуля к проекту

```kotlin
// settings.gradle.kts
include(":features:document-viewer")

// build.gradle.kts приложения
implementation(project(":features:document-viewer"))
implementation(project(":core:document:impl"))
```

Приложению нужен Hilt и `DocumentReader` в графе: `DocumentCore(context).reader` как `@Singleton`.

## Описание API

| Метод | Описание |
|---|---|
| `DocumentViewerFeatureApi.DocumentViewerScreen(file, modifier, onBackClick)` | Экран просмотра файла `file`; на каждый файл своя ViewModel |

## Примеры использования

```kotlin
composable(
    route = "file_viewer?path={path}",
    arguments = listOf(navArgument("path") { type = NavType.StringType })
) { entry ->
    documentViewer.DocumentViewerScreen(
        file = File(entry.arguments?.getString("path").orEmpty()),
        modifier = Modifier,
        onBackClick = { navController.popBackStack() }
    )
}
```
