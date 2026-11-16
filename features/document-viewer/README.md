# Просмотр документов

## Описание

Экран, который показывает файл приложения: PDF, офисные документы, текст, картинки, аудио и видео. Сам файл читает `:core:document`, фича только показывает результат и объясняет ошибку, если файл открыть нельзя.

## Архитектура и стек

```
features/document-viewer/
├── src/main/kotlin/com/android/appmanager/feature/documentviewer/
│   ├── DocumentViewerFeatureApi.kt        # публичный контракт фичи
│   ├── DocumentViewerFeatureImpl.kt       # его реализация, internal: ViewModel на каждый файл
│   ├── data/player/                       # PlayerFactory: Media3 ExoPlayer для аудио и видео
│   └── presentation/
│       ├── DocumentViewerFeatureScreen.kt # Route: ViewModel по File, пауза при уходе в фон
│       ├── DocumentViewerViewModel.kt     # открывает документ, ошибки; владеет документом и плеерами
│       ├── AudioPlayback.kt               # состояние аудиоплеера: прогресс, play/pause/seek
│       ├── model/                         # UiState: Loading / Loaded(document) / Error
│       └── screen/                        # экран и по экрану на вид документа
└── src/main/res/values/strings.xml        # строки с префиксом viewer_
```

* **Слой:** Presentation.
* **Ключевые технологии:** Jetpack Compose, Material 3, Media3 ExoPlayer, WebView + `WebViewAssetLoader`.
* **Зависимости:** `:core:document:api`, `:core:common-ui`, `:core:logger:client:api`. Нажатия пользователя уходят в `ActionLogger` из `:core:logger:client:api` (`UiActionLog.kt`, экран `document_viewer`); имена файлов в лог не пишутся. `DocumentReader` фича получает при создании: его даёт `:core:document:impl`.

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

### Как показывается каждый вид

| Документ | Экран |
|---|---|
| `PdfDocument` | Страницы списком, каждая рисуется под ширину экрана, когда становится видна |
| `HtmlDocument` | WebView; страницы документов и слайды целиком вписаны по ширине экрана в исходных пропорциях, таблицы в 100%, у таблиц вкладки по листам. Файлы отдаёт `WebViewAssetLoader`: без сети и доступа к файлам, остальные запросы блокируются |
| `TextDocument` | Текст построчно в ленивом списке, сверху кодировка и признак обрезки |
| `ImageDocument` | Картинка с pinch-zoom и двойным тапом; GIF и WebP анимируются на Android 9+ |
| `AudioDocument` | Обложка, название, плеер с перемоткой |
| `VideoDocument` | Media3 `PlayerView` с кнопками управления |

Кнопка в верхней панели открывает исходный файл в другом приложении (системный выбор), чтобы сравнить с вьювером. Файл отдаёт `FileProvider` из манифеста модуля, ему доступна только папка `filesDir`.

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

DI-фреймворк приложению не нужен: фичу создаёт функция `DocumentViewerFeatureApi(context, reader)`, одна на приложение. `reader` — `DocumentCore(context).reader` из `:core:document:impl`, тоже один на приложение.

## Описание API

| Метод | Описание |
|---|---|
| `DocumentViewerFeatureApi(context, reader): DocumentViewerFeatureApi` | Создаёт фичу; `reader` читает файлы |
| `DocumentViewerFeatureApi.DocumentViewerScreen(file, modifier, onBackClick)` | Экран просмотра файла `file`; на каждый файл своя ViewModel |

## Примеры использования

```kotlin
class App : Application() {
    val documentViewer: DocumentViewerFeatureApi by lazy { DocumentViewerFeatureApi(this, DocumentCore(this).reader) }
}

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
