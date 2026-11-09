# Файловый менеджер

## Описание

Экран приватного хранилища приложения (`Context.filesDir`): папки и файлы, импорт с устройства, экспорт, удаление и создание папок. Хранилище видно только приложению, поэтому разрешения не нужны: внешние файлы приходят и уходят через системные окна выбора (Storage Access Framework). Открывать файл фича не умеет: она отдаёт его наружу, а хост решает, чем показать.

## Архитектура и стек

```
features/file-manager/
├── src/main/kotlin/com/android/appmanager/feature/filemanager/
│   ├── FileManagerFeatureApi.kt        # публичный контракт фичи
│   ├── FileManagerFeatureImpl.kt       # его реализация, internal
│   ├── di/                             # Hilt-модуль: API и корень хранилища (filesDir)
│   ├── domain/
│   │   ├── model/                      # StoragePath, FileEntry, DirectoryContent, TransferSummary, DirectoryNameError
│   │   └── interactor/                 # FileManagerInteractor: сортировка, откат к существующей папке, проверка имени
│   ├── data/
│   │   ├── source/                     # InternalFileSource (защита от выхода за корень), ExternalDocumentSource (SAF)
│   │   └── repository/                 # FileStorageRepository: файлы, импорт, экспорт, откат частичной копии
│   ├── presentation/
│   │   ├── FileManagerFeatureScreen.kt # Route: ViewModel, системные пикеры, Back, snackbar
│   │   ├── FileManagerViewModel.kt     # состояние экрана, режим выбора, операции по одной
│   │   ├── FileManagerActions.kt       # действия экрана, их реализует ViewModel
│   │   ├── model/ mapper/ contract/    # UiState, тексты из ресурсов, «Сохранить как» с MIME по имени файла
│   │   └── screen/                     # FileManagerScreen, строка списка, диалоги
│   └── util/                           # MIME-тип по имени файла
└── src/main/res/values/strings.xml     # строки с префиксом fm_
```

* **Слой:** Presentation / Domain / Data.
* **Ключевые технологии:** Jetpack Compose, Material 3, Hilt, Storage Access Framework (`DocumentFile`), Kotlin Coroutines.
* **Зависимости:** `:core:common-ui`.

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

Логи: `adb logcat -s FileManager FileStorage`.

## Подключение модуля к проекту

```kotlin
// settings.gradle.kts
include(":features:file-manager")

// build.gradle.kts приложения
implementation(project(":features:file-manager"))
```

Фича использует Hilt: приложению нужен `@HiltAndroidApp` и `@AndroidEntryPoint` на Activity.

## Описание API

| Метод | Описание |
|---|---|
| `FileManagerFeatureApi.FileManagerScreen(modifier, onBackClick, onFileOpen)` | Экран фичи. Сам обрабатывает системный Back, пока открыта не корневая папка или выделены элементы |
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
```
