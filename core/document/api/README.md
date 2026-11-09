# Чтение документов: API

## Описание

Контракт чтения файлов приложения для показа: PDF, офисные документы, текст, картинки, аудио и видео. Модуль только описывает, что можно получить из файла; читает их `:core:document:impl`. Фичи зависят от этого модуля и не видят реализацию с её нативными библиотеками.

## Архитектура и стек

```
core/document/api/
└── src/main/kotlin/com/android/core/document/api/
    ├── DocumentReader.kt          # read(file, password): Document
    ├── DocumentFormat.kt          # формат по расширению файла
    ├── DocumentReadException.kt   # ошибка чтения с причиной Reason
    └── document/                  # Document (sealed) и его виды
        ├── PdfDocument.kt
        ├── HtmlDocument.kt
        ├── TextDocument.kt
        ├── ImageDocument.kt
        ├── AudioDocument.kt
        └── VideoDocument.kt
```

* **Слой:** Domain — только интерфейсы и модели, без логики.
* **Ключевые технологии:** Kotlin Coroutines (`suspend`), Android `Bitmap` / `Drawable` в результатах.
* **Зависимости:** не зависит от других модулей проекта.

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

## Подключение модуля к проекту

```kotlin
// settings.gradle.kts
include(":core:document:api")

// build.gradle.kts модуля, которому нужно читать документы
implementation(project(":core:document:api"))
```

Реализацию `DocumentReader` даёт `:core:document:impl`; её подключает приложение-хост.

## Описание API

| Тип | Что даёт |
|---|---|
| `DocumentReader.read(file, password = null): Document` | Открывает файл. Формат — по расширению. Тяжёлая работа идёт не на главном потоке. Документ нужно закрыть. |
| `DocumentFormat.of(file): DocumentFormat?` | Формат по расширению: по значению на тип (`PDF`, `DOCX`, `PNG`, `MP3`, `MP4`, …), `null` — не поддерживается. |
| `DocumentReadException.reason` | `NOT_FOUND`, `UNSUPPORTED_FORMAT`, `CORRUPTED`, `PASSWORD_REQUIRED`, `INVALID_PASSWORD`, `TOO_LARGE`, `IO_ERROR`. Других исключений `read` и методы документов не бросают. |

`Document` — sealed, у него шесть видов:

| Вид | Для чего | Что внутри |
|---|---|---|
| `PdfDocument` | PDF | `pageCount`, `pageSize(index)`, `renderPage(index, widthPx): Bitmap` |
| `HtmlDocument` | офисные документы, Markdown, SVG | `directory` и `pages` — HTML для WebView, у таблиц страница на лист |
| `TextDocument` | txt, log, json, xml | `text`, `charsetName`, `isTruncated` |
| `ImageDocument` | png, jpg, gif, webp, bmp, heif | `widthPx`, `heightPx`, `decode(maxW, maxH): Bitmap`, `decodeAnimation(): Drawable?` |
| `AudioDocument` | mp3, wav, m4a, aac, ogg, opus, flac | `file`, `durationMs`, `title`, `artist`, `album`, `loadArtwork(maxSizePx)` |
| `VideoDocument` | mp4, m4v, mov, 3gp, webm, mkv | `file` — играет просмотрщик |

Битмапы, которые возвращают документы, принадлежат вызывающему. Файлы `HtmlDocument` лежат в кэше реализации: их можно читать, но не менять и не удалять.

## Примеры использования

```kotlin
try {
    reader.read(file).use { document ->
        when (document) {
            is PdfDocument -> showPages(document.pageCount) { index -> document.renderPage(index, widthPx) }
            is HtmlDocument -> webView.loadPage(document.directory, document.pages.first())
            is TextDocument -> showText(document.text)
            is ImageDocument -> showImage(document.decode(maxWidthPx, maxHeightPx))
            is AudioDocument -> play(document.file)
            is VideoDocument -> play(document.file)
        }
    }
} catch (error: DocumentReadException) {
    if (error.reason == DocumentReadException.Reason.PASSWORD_REQUIRED) askPassword()
}
```
