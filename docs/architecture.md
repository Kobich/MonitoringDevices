# Архитектура

Ветка `feature/file-manager` — чистый проект, не зависит от `master`.
`app` показывает стартовый экран с кнопкой перехода в фичу файлового менеджера.

## Модули

```text
:app
  Точка входа, DI-сборка (Koin), навигация (Navigation Compose): home -> file_manager.

:feature:filemanager:api
  FileManagerFeatureApi — внешний контракт фичи (Composable-экран + onBackClick).

:feature:filemanager:impl
  Реализация: просмотр приватного хранилища приложения (Context.filesDir),
  импорт / экспорт через Storage Access Framework, удаление, создание папок.

:core:document:api
  Контракты чтения документов для просмотра, без привязки к библиотекам.
  Реализация (:core:document:impl) пока не выбрана.
```

```text
app -> feature:filemanager:impl -> feature:filemanager:api
```

## `core:document:api`

```text
DocumentSource          LocalFile(File) | Content(Uri, name, mimeType)
DocumentType            PDF, TEXT_DOCUMENT, SPREADSHEET, PRESENTATION, PLAIN_TEXT, IMAGE, UNSUPPORTED
DocumentTypeResolver    категория по MIME / расширению, без чтения содержимого
DocumentReadException   единая ошибка с reason (NOT_FOUND, CORRUPTED, PASSWORD_REQUIRED, ...)

html/HtmlDocumentRenderer  render(source, password) -> RenderedDocument (папка + страницы HTML), кэш
pdf/PdfReader              open(source, password) -> PdfDocument: pageCount, renderPage(...): Bitmap
text/TextReader            read(source, maxChars) -> TextDocument
image/ImageReader          decode(source, maxW, maxH): Bitmap, size(source)
```

Маршрутизация в будущем просмотрщике:

```text
TEXT_DOCUMENT, SPREADSHEET, PRESENTATION -> HtmlDocumentRenderer -> WebView
PDF                                      -> PdfReader -> страницы-Bitmap
PLAIN_TEXT                               -> TextReader
IMAGE                                    -> ImageReader
```

### План реализации `:core:document:impl`

- `HtmlDocumentRenderer` поверх OpenDocument.core (`app.opendocument:odr-core-android`, MPL-2.0):
  `Odr.open(path)` -> `Html.translate(file, HtmlConfig())` -> `bringOffline(outputDir)`.
  Нативные вызовы на `Dispatchers.IO` и под `Mutex`, пока не подтверждена потокобезопасность.
- Кэш: `cacheDir/rendered/<hash(path, size, lastModified)>/`, атомарно через временную папку.
  `Content`-источники сначала копируются во временный файл (odr принимает путь).
- Требует **minSdk 26** — поднять у всех модулей при подключении.
- Показ: WebView + `androidx.webkit.WebViewAssetLoader` с `InternalStoragePathHandler` на папку
  кэша. Так не нужны `HttpServer`, разрешение `INTERNET`, cleartext-конфиг и `allowFileAccess`.
  Запросы к остальным хостам блокировать в `shouldInterceptRequest`, без `addJavascriptInterface`:
  содержимое документов недоверенное.
- `PdfReader` — системный `PdfRenderer`; odr тоже умеет PDF, можно сравнить качество.

## Структура `feature:filemanager:impl`

```text
FileManagerFeatureImpl            реализация FileManagerFeatureApi
di/
  FileManagerFeatureModule        Koin-граф фичи; здесь задаётся корень хранилища
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
