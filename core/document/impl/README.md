# Чтение документов: реализация

## Описание

Реализация `DocumentReader` из `:core:document:api`: открывает файлы приложения офлайн, без сети и без отправки куда-либо. Офисные форматы переводит в HTML через библиотеку OpenDocument.core, остальное читает системными API Android.

## Архитектура и стек

```
core/document/impl/
├── src/main/kotlin/com/android/core/document/impl/
│   ├── DocumentCore.kt             # единственная публичная точка: DocumentCore(context).reader
│   ├── CompositeDocumentReader.kt  # выбор ридера по расширению, перевод ошибок в DocumentReadException
│   ├── FormatReader.kt             # интерфейс ридера одного или нескольких форматов
│   ├── ReadErrors.kt               # mapReadErrors: IOException / OutOfMemoryError -> DocumentReadException
│   ├── pdf/                        # PdfReader: PdfRenderer
│   ├── text/                       # TextReader, TextDecoding (кодировки), MarkdownReader
│   ├── image/                      # ImageReader, BitmapSampling
│   ├── svg/                        # SvgReader: страница с <img> для WebView
│   ├── audio/                      # AudioReader: метаданные и обложка
│   ├── video/                      # VideoReader: дорожки; без видео, но со звуком — как аудио
│   ├── office/                     # OfficeReader, OdrHtmlConverter (единственный класс с OpenDocument.core), правки файла до конвертации (DocumentPatch)
│   ├── cache/                      # RenderCache: результаты конвертации на диске
│   ├── chart/                      # диаграммы docx/pptx -> картинки
│   └── ooxml/                      # чтение и запись пакетов Office Open XML
└── src/main/AndroidManifest.xml    # tools:overrideLibrary для OpenDocument.core
```

* **Слой:** Data.
* **Ключевые технологии:** OpenDocument.core (`app.opendocument:odr-core-android` 7.7.0, MPL-2.0), `PdfRenderer`, `BitmapFactory` / `ImageDecoder`, `ExifInterface`, `MediaMetadataRetriever`, Kotlin Coroutines.
* **Зависимости:** `:core:document:api` (как `api`, поэтому подключающему модулю он виден сразу).

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

Ограничения по версиям Android:

| Что | С какой версии | Ниже |
|---|---|---|
| Офисные форматы | 8 (API 26), так требует OpenDocument.core | `UNSUPPORTED_FORMAT`; Markdown открывается как текст |
| HEIF, анимация GIF и WebP | 9 (API 28) | HEIF — `UNSUPPORTED_FORMAT`, анимация — первый кадр |

### Поддерживаемые форматы

| Ридер | Форматы | Результат |
|---|---|---|
| `PdfReader` | pdf | `PdfDocument` |
| `TextReader` | txt, log, json, xml | `TextDocument`, кодировка определяется сама: BOM, UTF-8, UTF-16, cp1251, Latin-1 |
| `MarkdownReader` | md, markdown | `HtmlDocument`, при ошибке — `TextDocument` |
| `ImageReader` | png, jpg, gif, webp, bmp, heif | `ImageDocument` с учётом EXIF-ориентации |
| `SvgReader` | svg | `HtmlDocument`: SVG показывается через `<img>`, его скрипты не выполняются |
| `AudioReader` | mp3, wav, m4a, aac, ogg, opus, flac | `AudioDocument` |
| `VideoReader` | mp4, m4v, mov, 3gp, webm, mkv | `VideoDocument` или `AudioDocument`, если видео нет |
| `OfficeReader` | docx, doc, odt, rtf, pages, xlsx, xls, ods, csv, numbers, pptx, ppt, odp, key, odg | `HtmlDocument` |

### Офисные документы

- Результат конвертации кэшируется в `cacheDir/documents`, не больше 200 MB: давно не открытые удаляются первыми. Изменённый файл получает новую запись.
- Зашифрованные файлы (PDF и офисные документы с паролем) не открываются: `UNSUPPORTED_FORMAT`.
- Текстовые документы показываются страницами, как в оригинале (`textDocumentMargin`): страница целиком, с текстом, картинками и таблицами в исходных пропорциях. Документы, слайды и рисунки открываются вписанными по ширине экрана: масштаб по ширине страницы ставит и при повороте пересчитывает сам WebView (`HtmlViewportMode.FIT_WIDTH`, нужны `useWideViewPort` и `loadWithOverviewMode`). Не `FIT_WIDTH_BY_VIEW`: его скрипт меряет экран один раз после загрузки, и WebView, получивший размер позже, открывает документ увеличенным. Таблицы открываются в 100% и листаются. После смены настроек вывода поднимайте `CACHE_VERSION` в `RenderCache`, иначе останутся старые результаты.
- В каждую страницу дописывается стиль `EXTRA_CSS` из `OdrHtmlConverter`: слово длиннее строки переносится посреди слова, как в Word, а не вылезает за страницу.
- То, чего OpenDocument.core не показывает, исправляется в копии файла до конвертации: `OfficeReader` прогоняет по порядку свои `DocumentPatch`. Упавшая правка пропускается, документ открывается без неё.
- OpenDocument.core не рисует диаграммы. Для docx и pptx каждая диаграмма заменяется картинкой того же размера (столбцы, линии, области, круговые, кольцевые, точечные). Нераспознанные диаграммы остаются как есть.
- OpenDocument.core теряет отступ первой строки и выступ. `DocxFirstLineIndents` считает их для каждого абзаца docx, как Word (абзац, стиль и его `basedOn`, стиль по умолчанию, `docDefaults`), и ставит в начало абзаца скрытую закладку с отступом в имени. После конвертации по этим закладкам дописываются правила `text-indent`. Элементы списков не трогаются: их раскладывает библиотека. Нужен WebView с CSS `:has()` (Chromium 105+), на более старом отступа просто нет.
- Размер нативных библиотек на устройстве — около 10.7 MB на arm64 (около 3.9 MB при загрузке из Play). Публикуйте AAB, чтобы устройство получало одну ABI.

Логи: `adb logcat -s DocumentReader RenderCache OdrHtmlConverter OfficeReader`.

## Подключение модуля к проекту

```kotlin
// settings.gradle.kts
include(":core:document:api")
include(":core:document:impl")

// build.gradle.kts приложения
implementation(project(":core:document:impl"))
```

DI-фреймворка внутри нет: создайте один `DocumentCore` на процесс, например в Hilt-модуле приложения.

## Описание API

| Метод | Описание |
|---|---|
| `DocumentCore(context)` | Собирает все ридеры; один на процесс, они делят кэш и очередь конвертации |
| `DocumentCore.reader: DocumentReader` | Готовый `DocumentReader`, см. `:core:document:api` |

## Примеры использования

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DocumentCoreModule {
    @Provides
    @Singleton
    fun provideDocumentReader(@ApplicationContext context: Context): DocumentReader = DocumentCore(context).reader
}
```
