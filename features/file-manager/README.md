# Файловый менеджер

## Описание

Экран приватного хранилища приложения: папки и файлы, импорт с устройства, экспорт, удаление и создание папок. Хранилище видно только приложению, поэтому разрешения не нужны: внешние файлы приходят и уходят через системные окна выбора (Storage Access Framework). Открывать файл фича не умеет: она отдаёт его наружу, а хост решает, чем показать.

У каждой фичи, которая хранит файлы, своё хранилище `FileStorage` — папка `filesDir/storage/<имя>`. Менеджер открывается на корне выбранного хранилища и выше него не поднимается. Без хранилища открывается собственное хранилище менеджера, `FileStorage.Default` (папка `file-manager`).

## Архитектура и стек

```
features/file-manager/
├── src/main/kotlin/com/android/appmanager/feature/filemanager/
│   ├── FileManagerFeatureApi.kt        # публичный контракт фичи
│   ├── FileManagerFeatureImpl.kt       # его реализация, internal
│   ├── FileStorage.kt                  # хранилище фичи: имя и папка filesDir/storage/<имя>
│   ├── di/                             # Hilt-модуль: API
│   ├── domain/
│   │   ├── model/                      # StoragePath, FileEntry, DirectoryContent, TransferSummary, DirectoryNameError
│   │   └── interactor/                 # FileManagerInteractor: сортировка, откат к существующей папке, проверка имени
│   ├── data/
│   │   ├── source/                     # InternalFileSource (защита от выхода за корень), ExternalDocumentSource (SAF)
│   │   └── repository/                 # FileStorageRepository: файлы, импорт, экспорт, откат частичной копии
│   ├── presentation/
│   │   ├── FileManagerFeatureScreen.kt # Route: ViewModel, системные пикеры, Back, snackbar
│   │   ├── FileManagerViewModel.kt     # состояние экрана, режим выбора, операции по одной; своя на каждое хранилище
│   │   ├── FileManagerActions.kt       # действия экрана, их реализует ViewModel
│   │   ├── model/ mapper/ contract/    # UiState, тексты из ресурсов, «Сохранить как» с MIME по имени файла
│   │   └── screen/                     # FileManagerScreen, строка списка, диалоги
│   └── util/                           # MIME-тип по имени файла
└── src/main/res/values/strings.xml     # строки с префиксом fm_
```

* **Слой:** Presentation / Domain / Data.
* **Ключевые технологии:** Jetpack Compose, Material 3, Hilt, Storage Access Framework (`DocumentFile`), Kotlin Coroutines.
* **Зависимости:** `:core:common-ui`, `:core:logger:client:api`, `:core:file-access` (конфиг администратора). Нажатия пользователя уходят в `ActionLogger` из `:core:logger:client:api` (`UiActionLog.kt`, экран `file_manager`); имена файлов в лог не пишутся.

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

### Поведение

| Жест / действие | Обычный режим | Режим выбора |
|---|---|---|
| Тап по папке | открыть папку | выделить / снять выделение |
| Тап по файлу | `onFileOpen(file)` наружу | выделить / снять выделение |
| Зажатие | войти в режим выбора, элемент выделен | выделить / снять выделение |
| Back / стрелка | на папку вверх; в корне — выход из фичи | выход из режима выбора |
| Верхняя панель | ⋮: импорт файлов, импорт папки, новая папка | «N selected», удалить, ⋮: выбрать все, экспорт |

- Экспорт одного файла открывает «Сохранить как», папки или нескольких элементов — выбор папки, куда всё копируется.
- Совпадающие имена при импорте получают суффикс: `name (1).ext`.
- Операции идут по одной, после каждой папка перечитывается. Если копирование оборвалось, уже скопированное удаляется.
- Удаление — после подтверждения, по одному элементу; останавливается на первой ошибке.
- Текущая папка и ожидающий экспорт переживают убийство процесса, пока открыт системный пикер.

### Конфиг администратора

Конфиг приходит из `FileAccessRules` модуля `:core:file-access` и может смениться, пока экран открыт. Без конфига доступно всё и без паролей.

| В конфиге | Что делает менеджер |
|---|---|
| действия нет в `menu` | пункт меню скрыт; если нет ни экспорта, ни удаления, режима выбора нет |
| у действия есть `password` | пароль спрашивается при каждом импорте, экспорте или удалении |
| у файла есть `message` | вместо открытия — диалог с сообщением, у файла значок ⓘ |
| у файла есть `password` | у файла замок, пароль спрашивает просмотрщик при каждом открытии |

Путь файла в конфиге начинается с имени хранилища: `file-manager/Договор.pdf`. Когда конфиг меняется, список и меню перерисовываются сразу, открытый диалог пароля закрывается, а начатая операция или уже открытый системный пикер доводятся до конца.

Логи: `adb logcat -s FileManager FileStorage`.

Как файловый менеджер, просмотрщик и `:core:document` работают вместе, какие форматы открываются и какие у них ограничения — в статье [WIKI.md](WIKI.md).

## Подключение модуля к проекту

```kotlin
// settings.gradle.kts
include(":features:file-manager")

// build.gradle.kts приложения
implementation(project(":features:file-manager"))
```

Фича использует Hilt: приложению нужен `@HiltAndroidApp` и `@AndroidEntryPoint` на Activity.

Внедрение в приложение с Hilt по шагам — [docs/hilt-integration.md](../../docs/hilt-integration.md).

## Описание API

| Метод | Описание |
|---|---|
| `FileManagerFeatureApi.FileManagerScreen(storage, modifier, onBackClick, onFileOpen)` | Экран хранилища `storage`. Сам обрабатывает системный Back, пока открыта не корневая папка или выделены элементы |
| `FileManagerFeatureApi.FileManagerScreen(modifier, onBackClick, onFileOpen)` | То же для `FileStorage.Default` |
| `FileStorage.named(name)` | Хранилище фичи. Имя — папка: 1–64 символа, строчные латинские буквы, цифры, `-` и `_`, первый символ буква или цифра; иначе `IllegalArgumentException`. Одинаковое имя — одна и та же папка |
| `FileStorage.Default` | Собственное хранилище менеджера, папка `file-manager` |
| `FileStorage.directory(context): File` | Папка хранилища, чтобы фича сама клала в неё файлы. Создаётся при первом открытии менеджером, до этого её может не быть |
| `onBackClick: () -> Unit` | Пользователь хочет выйти из фичи |
| `onFileOpen: (File) -> Unit` | Пользователь нажал на файл; хост решает, чем его показать |

## Примеры использования

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var fileManager: FileManagerFeatureApi
    // ...
}

composable("file_manager") {
    fileManager.FileManagerScreen(
        modifier = Modifier,
        onBackClick = { navController.popBackStack() },
        onFileOpen = { file -> navController.navigate("file_viewer?path=${Uri.encode(file.absolutePath)}") }
    )
}

// Хранилище другой фичи: менеджер покажет только filesDir/storage/reports
private val Reports = FileStorage.named("reports")

composable("reports") {
    fileManager.FileManagerScreen(
        storage = Reports,
        modifier = Modifier,
        onBackClick = { navController.popBackStack() },
        onFileOpen = { file -> navController.navigate("file_viewer?path=${Uri.encode(file.absolutePath)}") }
    )
}

// Фича пишет файлы в своё хранилище сама
val report = File(Reports.directory(context).apply { mkdirs() }, "report.pdf")
```
