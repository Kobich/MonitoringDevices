# Архитектура

Ветка `feature/file-manager` — чистый проект, не зависит от `master`.
`app` показывает стартовый экран с кнопкой перехода в фичу файлового менеджера.

## Модули

```text
:app
  Точка входа, Hilt (@HiltAndroidApp, DocumentCoreModule), навигация (Navigation Compose).

:feature:filemanager:api
  FileManagerFeatureApi — внешний контракт фичи (Composable-экран + onBackClick).

:feature:filemanager:impl
  Реализация: просмотр приватного хранилища приложения (Context.filesDir),
  импорт / экспорт через Storage Access Framework, удаление, создание папок.

:core:common-ui
  Общий UI для переиспользования: тема (MonitoringDevicesTheme), значки (AppIcons),
  компоненты (BackTopAppBar, CenteredContent, LoadingContent, MessageContent).
  Не зависит от фич и core:document; фичи и app подключают его через implementation.

:core:document:api
  Контракты чтения документов для просмотра, без привязки к библиотекам.
  Реализация (:core:document:impl) пока не выбрана.
```

```text
app, feature:*:impl -> core:common-ui
app -> feature:filemanager:impl -> feature:filemanager:api
app -> feature:viewer:impl -> feature:viewer:api, core:document:api
app -> core:document:impl -> core:document:api, odr-core-android
```

`:feature:viewer` показывает документ по `File`: `UniversalDocumentReader` -> `when (document)`:
`HtmlDocument` — WebView, `PdfDocument` — список страниц-битмапов, `TextDocument` — текст построчно,
`ImageDocument` — картинка с pinch-zoom, `AudioDocument` — плеер.

## `core:document:api`

Один базовый ридер, от него по ридеру на формат. Результат — sealed-иерархия документов.

```text
DocumentReader<out D : Document>          formats, canRead(source), read(source, options): D
 ├─ pdf/PdfReader                          PDF                      -> PdfDocument
 ├─ word/DocxReader, DocReader,
 │       OdtReader, RtfReader              DOCX, DOC, ODT, RTF      -> HtmlDocument
 ├─ spreadsheet/XlsxReader, XlsReader,
 │       OdsReader, CsvReader              XLSX, XLS, ODS, CSV      -> HtmlDocument
 ├─ presentation/PptxReader, PptReader,
 │       OdpReader                         PPTX, PPT, ODP           -> HtmlDocument
 ├─ text/TextReader                        TEXT                     -> TextDocument
 ├─ image/ImageReader                      IMAGE                    -> ImageDocument
 ├─ audio/AudioReader                      AUDIO                    -> AudioDocument
 └─ UniversalDocumentReader(readers)       все перечисленные        -> Document

sealed Document : Closeable                name, format
 ├─ PdfDocument      pageCount, pageSize, renderPage(index, widthPx): Bitmap, pageText
 ├─ HtmlDocument     directory, pages: List<HtmlPage(title, file)>   -> WebView
 ├─ TextDocument     text, charsetName, isTruncated
 ├─ ImageDocument    widthPx, heightPx, decode(maxW, maxH): Bitmap
 └─ AudioDocument    source, durationMs, title, artist, album, sampleRateHz, channelCount, loadArtwork()

DocumentFormat     формат: category, extensions, mimeTypes; DocumentFormat.of(source)
DocumentCategory   PDF, TEXT_DOCUMENT, SPREADSHEET, PRESENTATION, PLAIN_TEXT, IMAGE
DocumentSource     LocalFile(File) | Content(Uri, name, mimeType)
ReadOptions        password
DocumentReadException(reason)   NOT_FOUND, UNSUPPORTED_FORMAT, CORRUPTED, PASSWORD_REQUIRED, ...
cache/RenderedDocumentCache     sizeBytes(), clear() для результатов конвертации
```

Использование в UI:

```kotlin
val document = universalReader.read(DocumentSource.LocalFile(file))
when (document) {
    is PdfDocument -> PdfPages(document)
    is HtmlDocument -> HtmlPages(document)
    is TextDocument -> PlainText(document)
    is ImageDocument -> Picture(document)
}
```

Каждый конкретный ридер сам объявляет свой формат (`DocxReader.formats == {DOCX}`),
`UniversalDocumentReader` не даёт зарегистрировать два ридера на один формат.

### Сравнение: диаграммы как есть / картинкой (ВРЕМЕННО)

OpenDocument.core 7.7.0 не рисует диаграммы DrawingML (docx/xlsx/pptx). Чтобы сравнить,
в просмотрщике для docx есть переключатель **Charts: As is / As picture**:

- **As is** — `OdrDocxReader`, файл уходит в OpenDocument.core без изменений.
- **As picture** — `ChartRasterizingDocxReader`: `DocxChartRasterizer` пишет копию docx, где каждая
  диаграмма (`a:graphicData` с `c:chart`) заменена картинкой `pic:pic` того же размера.
  Данные — из кэшей `c:numCache`/`c:strCache` (`ChartXmlParser`), цвета — из темы документа,
  рисует `AndroidChartImageRenderer` (столбцы/полосы с группировкой, линии, области, круговые,
  кольцевые, точечные; заголовок, легенда, оси). Нераспознанные диаграммы остаются как есть.
  Логи: `adb logcat -s ChartRasterizing`.

Результаты кэшируются раздельно (`variant` в ключе кэша), переключение не пересчитывает уже открытое.

**Примеры файлов (ВРЕМЕННО).** `app/.../sample/` при первом запуске кладёт в `filesDir`:
`Readme.txt`, `Quarterly report.docx` (таблица, столбчатая и линейная диаграммы), `Budget.csv`,
папку `Samples/` с `Market share.docx` (круговая и полосовая диаграммы), `Sales by region.csv`,
`Notes.md` и пустой папкой, папку `Media/` с `Sample.pdf`, `Picture.png`, `Chord.wav` и текстами
в UTF-8 и cp1251. Один раз на версию набора, флаг в SharedPreferences `sample_files`.
Перед релизом удалить пакет `sample` и его вызов в `MonitoringDevicesApplication`.

**Уборка после выбора.**
Общее: удалить `core/document/api/.../comparison/`, `DefaultDocumentReaderVariants`,
`ChartRenderingSwitch` и поле `chartRendering` во viewer; `OpenDocumentInteractor` берёт
`UniversalDocumentReader`, `DocumentCore` собирает его из списка ридеров.

- Остались на **As is**: удалить `chart/` и `ChartRasterizingDocxReader` с тестами.
- Остались на **As picture**: удалить `OdrDocxReader`, `ChartRasterizingDocxReader` переименовать
  в `OdrDocxReader`; при желании применить тот же подход к xlsx/pptx.

### Реализации в `:core:document:impl`

| Ридер | Форматы | Чем | Открытые вопросы (выбрать позже) |
| --- | --- | --- | --- |
| `Odr*Reader` (11 шт.) | docx, doc, odt, rtf, xlsx, xls, ods, csv, pptx, ppt, odp | OpenDocument.core -> HTML | диаграммы (сравнение), minSdk 26 |
| `AndroidPdfReader` | pdf | `PdfRenderer` | пароль только на API 35+, нет текста/поиска; zoom в UI. Варианты: androidx.pdf, pdfium |
| `AndroidTextReader` | txt, md, json, xml, log | свой код (`TextDecoding`) | эвристика cp1251/Latin-1 -> ICU `CharsetDetector`/juniversalchardet; подсветка md/json |
| `AndroidImageReader` | png, jpg, gif, webp, bmp, heif | `BitmapFactory` + `ExifInterface` | анимация GIF/WebP, большие фото (тайлинг) -> Coil/subsampling |
| `AndroidAudioReader` | mp3, wav, m4a, aac, ogg, opus, flac | `MediaMetadataRetriever` + `MediaExtractor` | — |

Плеер в просмотрщике: интерфейс `AudioPlayer`, сейчас `MediaPlayerAudioPlayer`; пауза при уходе
экрана в фон. Фоновое воспроизведение, уведомление, audio focus -> Media3 (ExoPlayer).

### План реализации `:core:document:impl`

- Сделано: офисные ридеры (`DocxReader`, `XlsxReader`, `PptxReader`, ...) поверх одного общего класса на
  OpenDocument.core (`app.opendocument:odr-core-android`, MPL-2.0, проверена 7.7.0):
  `Odr.open(path)` -> `decrypt(password)` при `passwordEncrypted()` ->
  `Html.translate(file, HtmlConfig())` -> `bringOffline(outputDir)` -> `pages()` = `HtmlPage(name, path)`.
  Нативные вызовы на `Dispatchers.IO` и под `Mutex`, пока не подтверждена потокобезопасность.
- Кэш: `cacheDir/rendered/<hash(path, size, lastModified)>/`, атомарно через временную папку.
  `Content`-источники сначала копируются во временный файл (odr принимает путь).
- Требует **minSdk 26**. Пока решение не принято: `tools:overrideLibrary` в манифестах
  `app` и `core:document:impl`, офисные ридеры регистрируются только на API 26+.
- Показ: WebView + `androidx.webkit.WebViewAssetLoader` с `InternalStoragePathHandler` на папку
  кэша. Так не нужны `HttpServer`, разрешение `INTERNET`, cleartext-конфиг и `allowFileAccess`.
  Запросы к остальным хостам блокировать в `shouldInterceptRequest`, без `addJavascriptInterface`:
  содержимое документов недоверенное.
- `PdfReader` — системный `PdfRenderer`; odr тоже умеет PDF, можно сравнить качество.
- Размер odr-core-android 7.7.0 (`libodr_jni.so` + `libc++_shared.so`):

  | ABI | в AAR (без strip) | после strip | сжато (загрузка) |
  | --- | --- | --- | --- |
  | arm64-v8a | 140 MB | 10.7 MB | ~3.9 MB |
  | armeabi-v7a | 117 MB | 7.8 MB | ~3.5 MB |
  | x86_64 / x86 | 135 / 116 MB | 11.1 / 10.5 MB | ~4.1 MB |

  AGP вырезает отладочные символы только при установленном NDK — без него в APK попадут
  неурезанные библиотеки. Публиковать AAB (или делать ABI splits), чтобы устройство получало одну ABI.

## Структура `feature:filemanager:impl`

```text
FileManagerFeatureImpl            реализация FileManagerFeatureApi
di/
  FileManagerFeatureModule        Hilt-модуль фичи: биндинги API и репозитория, корень хранилища
domain/
  model/                          StoragePath, FileEntry, DirectoryContent, TransferSummary, ошибки имени
  repository/FileStorageRepository  контракт доступа к хранилищу
  interactor/FileManagerInteractor  сортировка, fallback на существующую папку, валидация имени, правила удаления
data/
  source/InternalFileSource       StoragePath -> File, защита от выхода за корень, уникальные имена
  source/ExternalDocumentSource   SAF: чтение/запись документов и деревьев (DocumentFile)
  mapper/FileEntryMapper          File -> FileEntry, MIME по расширению
  repository/DefaultFileStorageRepository  копирование, удаление, откат частично скопированного
presentation/
  FileManagerFeatureScreen        Route: ViewModel, ActivityResult-лаунчеры, Back, snackbar
  FileManagerViewModel            UDF-состояние, SavedStateHandle (текущая папка, ожидающий экспорт)
  model/ mapper/ contract/        UiState, форматирование, CreateDocument с MIME на каждый запуск
  screen/                         FileManagerScreen, FileEntryRow, диалоги
```

## Поведение

Два режима экрана, решение о смысле жеста принимает `FileManagerViewModel`.

| Жест / действие | Обычный режим | Режим выбора |
| --- | --- | --- |
| Тап по папке | открыть папку | выделить / снять выделение |
| Тап по файлу | открыть файл (`onFileOpen(File)` наружу) | выделить / снять выделение |
| Зажатие | войти в режим выбора, элемент выделен | выделить / снять выделение |
| Вид строки | значок типа файла | круглая отметка выезжает слева от значка |
| Снятие последнего выделения, ✕, Back | — | выход в обычный режим |
| Back / стрелка | вверх; в корне — выход из фичи | выход из режима выбора |
| Хлебные крошки | переход в папку | недоступны |
| Верхняя панель | ⋮: импорт файлов, импорт папки, новая папка | «N selected», удалить, ⋮: выбрать все, экспорт |

Экспорт: один файл — системный «Сохранить как» (`CreateDocument`), иначе (папка или несколько
элементов) — выбор папки (`OpenDocumentTree`) и копирование внутрь. Правило в
`FileManagerInteractor.exportDestination`. Удаление — после подтверждения, по одному элементу,
останавливается на первой ошибке.

Открытие файла: фича отдаёт `File` через `FileManagerFeatureApi.onFileOpen`, `app` ведёт на
маршрут `file_viewer?path=...`. Сейчас там заглушка `FileViewerPlaceholderScreen`; её заменит
фича просмотрщика поверх `:core:document`.

Конфликты имён при импорте решаются как `name (1).ext`. Внешние имена санитизируются,
пути всегда проверяются на нахождение внутри корня.
Разрешения не нужны: `filesDir` приватен, внешние файлы доступны через SAF-пикеры.

## UI и UDF

```text
Route -> Screen(state, callbacks)
ViewModel -> UiState -> Screen
Screen -> callback -> ViewModel
```

Модифицирующие операции выполняются по одной (`isBusy`), после каждой папка перечитывается.
Папка также перечитывается на `ON_RESUME`.

## Тесты

`feature/filemanager/impl/src/test` — JVM unit-тесты `StoragePath`, `InternalFileSource`,
`FileManagerInteractor`:

```shell
./gradlew :feature:filemanager:impl:testDebugUnitTest
```

## `core:common-ui`

```text
theme/        MonitoringDevicesTheme, цвета, типографика
icons/        AppIcons: Back, Close, Delete, More, Add, Check (Material) + свои Folder, File (ImageVector)
              FileKind.of(name, isDirectory): FOLDER, PDF, DOCUMENT, SPREADSHEET, PRESENTATION,
                TEXT, IMAGE, AUDIO, VIDEO, ARCHIVE, OTHER (по расширению)
              FileTypeIcons / FileTypeIcon(kind, size): цветные векторные значки файлов
components/   BackTopAppBar(title, onNavigationClick, navigationIcon, actions)
              CenteredContent { ... }, LoadingContent(text), MessageContent(title, text, actionLabel)
              SelectionIndicator(selected): круглая отметка выбора с анимацией
```

Правила: значки в фичах только через `AppIcons` (не `Icons.*` и не drawable в фиче);
компонент переносится сюда, как только нужен второй фиче; без бизнес-логики и без зависимостей
от `feature:*` / `core:document`.

## DI

| Модули | DI | Как |
| --- | --- | --- |
| `core:*` | без фреймворка | `core:document:impl` отдаёт одну публичную точку сборки `DocumentCore(context, config)`, всё остальное `internal`; `core:common-ui` без зависимостей |
| `feature:*:impl` | Hilt | `@Inject constructor`, `@HiltViewModel`, `internal` Hilt-модуль `@InstallIn(SingletonComponent)` с `@Binds` для `*FeatureApi` |
| `app` | Hilt | `@HiltAndroidApp`, `@AndroidEntryPoint`; `di/DocumentCoreModule` создаёт `DocumentCore` как `@Singleton` и отдаёт его API-типы |

Параметры экрана, которые приходят через API фичи (например `File` у просмотрщика), передаются
assisted injection: `@HiltViewModel(assistedFactory = ...)` +
`hiltViewModel<VM, VM.Factory>(key = ..., creationCallback = { it.create(file) })`.

Версии: Hilt 2.60.1 (для AGP 9 нужен >= 2.59.2), KSP 2.3.13 (для встроенного Kotlin AGP 9 нужен >= 2.3.6).
