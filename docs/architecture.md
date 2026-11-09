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
  компоненты (BackTopAppBar, CenteredContent, LoadingContent, MessageContent, AppAlertDialog,
  AppProgressBar, ConnectionStatusIndicator).
  Не зависит от фич и core:document; фичи и app подключают его через implementation.

:core:document:api
  DocumentReader.read(file) -> Document. Без привязки к библиотекам.

:core:document:impl
  DocumentCore(context).reader: ридеры по форматам на системных API и OpenDocument.core.
```

```text
app, feature:*:impl -> core:common-ui
app -> feature:filemanager:impl -> feature:filemanager:api
app -> feature:viewer:impl -> feature:viewer:api, core:document:api
app -> core:document:impl -> core:document:api, odr-core-android
```

`:feature:viewer` показывает документ по `File`: `DocumentReader.read(file)` -> `when (document)`:
`HtmlDocument` — WebView, `PdfDocument` — список страниц-битмапов, `TextDocument` — текст построчно,
`ImageDocument` — картинка с pinch-zoom, `AudioDocument` — плеер.

## `core:document:api`

```kotlin
documentReader.read(file).use { document ->
    when (document) {
        is PdfDocument -> ...     // pageCount, renderPage(index, widthPx): Bitmap
        is HtmlDocument -> ...    // directory + pages -> WebView
        is TextDocument -> ...    // text, charsetName, isTruncated
        is ImageDocument -> ...   // decode(maxW, maxH): Bitmap, decodeAnimation()
        is AudioDocument -> ...   // file, durationMs, title, artist, loadArtwork()
    }
}
```

Весь API — 9 файлов:

```text
DocumentReader          read(file, password): Document
DocumentFormat          формат по расширению файла, по одному значению на тип файла (PNG, JPEG, MP3, DOCX, ...)
DocumentReadException   reason: NOT_FOUND, UNSUPPORTED_FORMAT, CORRUPTED, PASSWORD_REQUIRED, ...
document/               Document (sealed) + PdfDocument, HtmlDocument, TextDocument, ImageDocument, AudioDocument
```

Приложение офлайн и открывает только свои файлы, поэтому на входе всегда `File`: ни URI, ни MIME-типов.

## `core:document:impl`

Наружу — только `DocumentCore(context).reader`. Внутри `CompositeDocumentReader` берёт формат по
расширению и вызывает `FormatReader.read(file, format, password)` этого формата. Ошибки, которые ридер
не перевёл сам (`IOException`, нехватка памяти), он превращает в `DocumentReadException` (`mapReadErrors`),
так что наружу уходит только `DocumentReadException`; методы документов (`renderPage`, `decode`, ...)
делают то же самое.

| Ридер | Форматы | Чем | Открытые вопросы |
| --- | --- | --- | --- |
| `PdfReader` | pdf | `PdfRenderer` | пароль только на Android 15+, нет поиска/выделения текста, zoom в UI |
| `TextReader` | txt, json, xml, log | свой код (`TextDecoding`: BOM, UTF-8/16, cp1251/Latin-1) | эвристика кодировок -> ICU `CharsetDetector` |
| `MarkdownReader` | md, markdown | OpenDocument.core -> HTML; на Android 6–7 и при ошибке — как текст | — |
| `ImageReader` | png, jpg, gif, webp, bmp, heif | `BitmapFactory` + EXIF, декод с уменьшением под рамку; GIF/WebP-анимация — `ImageDecoder` (Android 9+); HEIF — Android 9+ | анимация на 6–8 |
| `SvgReader` | svg | страница с `<img>` в WebView (скрипты SVG не выполняются) | — |
| `AudioReader` | mp3, wav, m4a, aac, ogg, opus, flac | `MediaMetadataRetriever` + `MediaExtractor`, всё опционально | — |
| `OfficeReader` | docx, pptx (диаграммы картинками); doc, odt, rtf, pages, xlsx, xls, ods, csv, numbers, ppt, odp, key, odg | OpenDocument.core -> HTML, Android 8+ | диаграммы в xlsx; качество iWork/odg на реальных файлах |

Плеер в просмотрщике: интерфейс `AudioPlayer`, сейчас `MediaPlayerAudioPlayer`; пауза при уходе
экрана в фон. Фон, уведомление, audio focus -> Media3 (ExoPlayer).

### OpenDocument.core

- `app.opendocument:odr-core-android` 7.7.0, MPL-2.0. Единственный класс, который с ней говорит, —
  `OdrHtmlConverter`: `Odr.open(path)` -> `decrypt(password)` -> `Html.translate(...)` ->
  `bringOffline(outputDir, views)`. Виды: у таблиц только листы (вкладки при >1 листе), у остальных
  общий вид документа. Ссылки на ресурсы относительные. Нативные вызовы по одному (`Mutex`).
- Кэш `RenderCache`: `cacheDir/documents/<hash(путь, размер, время изменения, вариант)>/`, папка
  появляется атомарно. Не больше 200 MB: после каждой новой записи удаляются давно не открытые и
  временные папки, оставшиеся от убитого процесса. Документ, открытый с паролем, кэшируется под этим
  паролем (входит в хэш ключа), поэтому без пароля он заново спросит пароль. Расшифрованный HTML
  лежит в приватном `cacheDir`.
- Готовый документ берётся из кэша без ожидания `Mutex`: им ждут только конвертации.
- Библиотека требует Android 8 (API 26), приложение — minSdk 23: `tools:overrideLibrary` в манифестах
  `app` и `core:document:impl`, `OfficeReader` регистрируется только на API 26+.
- Показ: WebView + `WebViewAssetLoader` (`InternalStoragePathHandler` на папку результата): без сети,
  без доступа к файлам; остальные запросы блокируются, JS-интерфейсов нет.
- Размер (`libodr_jni.so` + `libc++_shared.so`) на устройстве после strip: arm64 10.7 MB
  (~3.9 MB при загрузке из Play). В AAR символы не вырезаны (140 MB) — их вырезает AGP при
  установленном NDK; публиковать AAB, чтобы устройство получало одну ABI.

### Диаграммы в docx и pptx

OpenDocument.core не рисует диаграммы DrawingML. `OfficeReader` для docx и pptx перед конвертацией
пишет копию файла, где каждая диаграмма заменена картинкой того же размера и положения:

- `ooxml/OoxmlPackage` — части, связи, типы контента, запись копии; `ooxml/OoxmlXml` — XML-хелперы;
- `chart/DocxChartRasterizer`: `a:graphicData` с `c:chart` -> `pic:pic`;
- `chart/PptxChartRasterizer`: на слайдах `p:graphicFrame` с `c:chart` -> `p:pic`;
- данные из кэшей `c:numCache`/`c:strCache` (`ChartXmlParser`), цвета из темы документа, рисует
  `ChartImageRenderer` (столбцы/полосы, линии, области, круговые, кольцевые, точечные).

Нераспознанные диаграммы остаются как есть, при ошибке рендерится исходный файл.
Логи: `adb logcat -s OfficeReader`.

## Структура `feature:filemanager:impl`

```text
FileManagerFeatureImpl            реализация FileManagerFeatureApi
di/
  FileManagerFeatureModule        Hilt-модуль фичи: биндинг API, корень хранилища
domain/
  model/                          StoragePath, FileEntry, DirectoryContent, TransferSummary, ошибки имени
  interactor/FileManagerInteractor  сортировка, fallback на существующую папку, валидация имени
data/
  source/InternalFileSource       StoragePath -> File, защита от выхода за корень, уникальные имена
  source/ExternalDocumentSource   SAF: чтение/запись документов и деревьев (DocumentFile)
  repository/FileStorageRepository  файлы хранилища, импорт/экспорт, откат частично скопированного;
                                    один класс без интерфейса: реализация одна
presentation/
  FileManagerFeatureScreen        Route: ViewModel, системные пикеры по `state.picker`, Back, snackbar
  FileManagerActions              действия экрана; реализует FileManagerViewModel, экран получает `actions = viewModel`
  FileManagerViewModel            UDF-состояние, SavedStateHandle (текущая папка, ожидающий экспорт)
  model/ mapper/ contract/        UiState, тексты из ресурсов (FileManagerUiMapper), CreateDocument с MIME на каждый запуск
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
`FileManagerViewModel.onExportSelectedClick`. Все четыре пикера (импорт файлов и папки, экспорт файла и в папку)
идут одним путём: ViewModel ставит `picker`, Route запускает его, результат приходит в свой обработчик.
Тексты — в `res/values/strings.xml` модуля (префикс `fm_`, количества — `plurals`).
Удаление — после подтверждения, по одному элементу,
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

## `core:common-ui`

```text
theme/        MonitoringDevicesTheme: системные цвета устройства (Material You на Android 12+,
                базовая палитра Material 3 ниже), светлая/тёмная по системе; типографика
icons/        AppIcons: Back, Close, Delete, More, Add, Check (Material) + свои Folder, File (ImageVector)
              FileKind.of(name, isDirectory): FOLDER, PDF, DOCUMENT, SPREADSHEET, PRESENTATION,
                TEXT, IMAGE, AUDIO, VIDEO, ARCHIVE, OTHER (по расширению)
              FileTypeIcons / FileTypeIcon(kind, size): цветные векторные значки файлов
components/   BackTopAppBar(title, onNavigationClick, navigationIcon, actions)
              CenteredContent { ... }, LoadingContent(text), MessageContent(title, text, actionLabel)
              SelectionIndicator(selected): круглая отметка выбора с анимацией
              AppAlertDialog(title, text | content { }, confirmLabel, dismissLabel, destructive, dismissible)
              AppProgressBar(progress: Float? (null = неизвестен), label, showPercent)
              ConnectionStatus: DISCONNECTED, CONNECTING, CONNECTED
              ConnectionStatusIndicator(status, label) / ConnectionStatusDot(status): плашка и точка,
                CONNECTED — primary, CONNECTING — tertiary с пульсацией, DISCONNECTED — error
```

Правила: цвета только из `MaterialTheme.colorScheme`, без захардкоженных `Color(0x...)` (кроме цветных
значков файлов); строки — в `res/values/strings.xml` модуля с префиксом `common_ui_`;
значки в фичах только через `AppIcons` (не `Icons.*` и не drawable в фиче);
компонент переносится сюда, как только нужен второй фиче; без бизнес-логики и без зависимостей
от `feature:*` / `core:document`.

## DI

| Модули | DI | Как |
| --- | --- | --- |
| `core:*` | без фреймворка | `core:document:impl` отдаёт одну публичную точку `DocumentCore(context).reader`, всё остальное `internal`; `core:common-ui` без зависимостей |
| `feature:*:impl` | Hilt | `@Inject constructor`, `@HiltViewModel`, `internal` Hilt-модуль `@InstallIn(SingletonComponent)` с `@Binds` для `*FeatureApi` |
| `app` | Hilt | `@HiltAndroidApp`, `@AndroidEntryPoint`; `di/DocumentCoreModule` отдаёт `DocumentReader` как `@Singleton` |

Параметры экрана, которые приходят через API фичи (например `File` у просмотрщика), передаются
assisted injection: `@HiltViewModel(assistedFactory = ...)` +
`hiltViewModel<VM, VM.Factory>(key = ..., creationCallback = { it.create(file) })`.

Версии: Hilt 2.60.1 (для AGP 9 нужен >= 2.59.2), KSP 2.3.13 (для встроенного Kotlin AGP 9 нужен >= 2.3.6).

## Примеры файлов (ВРЕМЕННО)

`app/.../sample/` при первом запуске кладёт в `filesDir` примеры: `Readme.txt`, `Quarterly report.docx`
(таблица, 2 картинки, столбчатая и линейная диаграммы, подписи «Picture N» / «Chart N»), `Budget.csv`,
`Samples/` (`Market share.docx`, `Sales by region.csv`, `Notes.md`, пустая папка), `Media/`
(`Sample.pdf`, `Picture.png`, `Chord.wav`, тексты в UTF-8 и cp1251). Один раз на версию набора
(SharedPreferences `sample_files`), новая версия перезаписывает примеры.
Перед релизом удалить пакет `sample` и его вызов в `MonitoringDevicesApplication`.
