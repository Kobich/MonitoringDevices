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
DocumentType            PDF, DOCX, TEXT, IMAGE, UNSUPPORTED
DocumentTypeResolver    формат по MIME / расширению, без чтения содержимого
DocumentReadException   единая ошибка с reason (NOT_FOUND, CORRUPTED, PASSWORD_REQUIRED, ...)

pdf/PdfReader           open(source, password) -> PdfDocument (Closeable)
pdf/PdfDocument         pageCount, pageSize, renderPage(index, widthPx): Bitmap, pageText
docx/DocxReader         read(source) -> RichTextDocument
richtext/               формато-независимая модель: Paragraph, Table, Image, PageBreak, стили
text/TextReader         read(source, maxChars) -> TextDocument (текст, кодировка, обрезан ли)
image/ImageReader       decode(source, maxW, maxH): Bitmap, size(source)
```

Все методы `suspend`, реализация сама уходит с main-потока. DOCX отдаётся в
`RichTextDocument`, чтобы ODT/RTF и т.п. позже ложились в ту же модель и тот же UI.

`app` знает только `FileManagerFeatureApi`; реализация приходит из Koin-модуля `fileManagerFeatureModule`.

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

| Действие | Где | Как |
| --- | --- | --- |
| Открыть папку | тап по папке, хлебные крошки | — |
| Вверх | стрелка в TopAppBar, системный Back | в корне — выход из фичи |
| Импорт файлов | меню ⋮ в TopAppBar | `OpenMultipleDocuments`, копия в текущую папку |
| Импорт папки | меню ⋮ в TopAppBar | `OpenDocumentTree`, рекурсивная копия |
| Новая папка | меню ⋮ в TopAppBar | диалог с валидацией имени |
| Экспорт | long press / ⋮ у элемента / тап по файлу | файл — `CreateDocument`, папка — `OpenDocumentTree` |
| Удалить | long press / ⋮ у элемента | подтверждение, папка удаляется рекурсивно |

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
