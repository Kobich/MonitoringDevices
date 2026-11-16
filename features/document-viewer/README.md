# Просмотр документов

## Описание

Экран, который показывает файл приложения: PDF, офисные документы, текст, картинки, аудио и видео. Сам файл читает `:core:document`, фича только показывает результат и объясняет ошибку, если файл открыть нельзя.

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
│       ├── DocumentViewerViewModel.kt     # открывает документ, ошибки; владеет документом и плеерами
│       ├── AudioPlayback.kt               # состояние аудиоплеера: прогресс, play/pause/seek
│       ├── model/                         # UiState: Loading / Locked / Unavailable / Loaded(document) / Error
│       └── screen/                        # экран и по экрану на вид документа
└── src/main/res/values/strings.xml        # строки с префиксом viewer_
```

* **Слой:** Presentation.
* **Ключевые технологии:** Jetpack Compose, Material 3, Hilt (assisted injection: ViewModel создаётся под `File`), Media3 ExoPlayer, WebView + `WebViewAssetLoader`.
* **Зависимости:** `:core:document:api`, `:core:common-ui`, `:core:logger:client:api`, `:core:file-access` (конфиг администратора). Нажатия пользователя уходят в `ActionLogger` из `:core:logger:client:api` (`UiActionLog.kt`, экран `document_viewer`); имена файлов в лог не пишутся. `DocumentReader` фича берёт из графа Hilt приложения: его даёт `:core:document:impl` (см. `DocumentCoreModule` в `:app`).

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

### Конфиг администратора

Просмотрщик сам применяет конфиг из `FileAccessRules` модуля `:core:file-access`, как бы его ни открыли: из менеджера, по пути из другого места или заново после смерти процесса. Пока не пришёл первый конфиг, показывается загрузка.

- **`message` у файла** — вместо файла показывается сообщение.
- **`password` у файла** — пароль спрашивается при каждом открытии экрана; «Отмена» закрывает просмотрщик.
- **«Открыть в другом приложении»** — это экспорт: кнопки нет, если `export` нет в `menu`, и спрашивается пароль экспорта, если он задан. Кнопка появляется только после того, как файл открыт.
- **Смена конфига** — если у открытого файла появился или сменился пароль или появилось сообщение, просмотрщик закрывается (`onBackClick`).

Путь в конфиге считается от `filesDir/storage`; у файлов вне этой папки правил файлов нет.

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

Приложению нужен Hilt и `DocumentReader` в графе: `DocumentCore(context).reader` как `@Singleton`.

Внедрение в приложение с Hilt по шагам — [docs/hilt-integration.md](../../docs/hilt-integration.md).

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
