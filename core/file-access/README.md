# Конфиг доступа к файлам

## Описание

Модуль даёт фичам конфиг администратора: какие действия файлового менеджера доступны и с каким паролем, какие файлы открываются только по паролю или вместо открытия показывают сообщение. Конфиг один на всё приложение и может меняться, пока экраны открыты: `:features:file-manager` и `:features:document-viewer` подписаны на него и применяют его сами.

Откуда приходит конфиг, модуль не знает: источник даёт приложение (сейчас заглушка в `:app`, потом модуль store). Без источника, без конфига и с битым конфигом доступно всё и без паролей.

## Архитектура и стек

```
core/file-access/src/main/kotlin/com/android/core/fileaccess/
├── FileAccessConfig.kt        # модель: действия с паролями, правила файлов, ActionAccess, FileAccess, Password
├── FileAccessConfigJson.kt    # разбор JSON администратора
├── FileAccessConfigSource.kt  # откуда конфиг: реализует приложение
├── FileAccessRules.kt         # текущий конфиг для фич: Flow, без источника — Unrestricted
├── AppFilesRoot.kt            # filesDir/storage и путь файла от него, как в конфиге
└── di/FileAccessModule.kt     # пустой набор источников, чтобы источник был необязательным
```

* **Ключевые технологии:** Kotlin Coroutines (`Flow`), Hilt, `org.json`.
* **Зависимости:** нет.

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

## Формат

```json
{
  "parameters": {
    "menu": [
      { "key": "import", "password": "" },
      { "key": "export", "password": "1111" },
      { "key": "delete", "password": "2222" }
    ],
    "initial_files": [
      { "path": "file-manager/Договор.pdf", "message": "", "password": "1234" },
      { "path": "file-manager/Бюджет.csv", "message": "Бюджет выдаёт администратор", "password": "" }
    ]
  }
}
```

| Поле | Значение |
|---|---|
| `menu` | доступные действия: `import` (файлы и папки), `export` (и «Открыть в другом приложении» просмотрщика), `delete`. Чего нет в списке, того нет в интерфейсе. Нет поля `menu` — доступно всё |
| `menu[].password` | пароль, спрашивается при каждом выполнении; пустой — без пароля |
| `initial_files[].path` | путь от `filesDir/storage`, начинается с имени хранилища: `file-manager/…`. Разделитель `/` или `\`, регистр важен, `..` нельзя |
| `initial_files[].message` | показывается вместо открытия файла; пароль тогда не спрашивается |
| `initial_files[].password` | пароль на просмотр, спрашивается при каждом открытии |

Не читаются: `code_sequence`, `select` (действие с `"select": false` считается отсутствующим) и любые другие поля. Плохая запись (неизвестный `key`, неверный путь, повтор) пропускается, остальные действуют; о каждой сообщает `onProblem` у `FileAccessConfigJson.parse`. JSON, который не разобрался совсем, — `null`, то есть «конфига нет».

## Описание API

| Метод | Описание |
|---|---|
| `FileAccessConfigSource.config: Flow<FileAccessConfig?>` | Реализует приложение. Должен сразу отдавать значение: экраны ждут первое, прежде чем показать закрытое. `null` и ошибка в потоке — «конфига нет»; после ошибки поток собирается заново через 5 с |
| `FileAccessRules.config: Flow<FileAccessConfig>` | Текущий конфиг для фич; без источника — `FileAccessConfig.Unrestricted` |
| `FileAccessConfigJson.parse(json, onProblem): FileAccessConfig?` | Разбор формата выше |
| `FileAccessConfig.access(action): ActionAccess` | `Allowed`, `Hidden` или `PasswordRequired` |
| `FileAccessConfig.access(path): FileAccess` | `Allowed`, `Message` или `PasswordRequired` |
| `Password.matches(input)` | Сравнение пароля; `toString()` пароль не показывает |
| `AppFilesRoot.directory(context)` / `pathOf(file, root)` | Корень файлов приложения и путь файла от него, как в конфиге |

## Подключение модуля к проекту

```kotlin
// settings.gradle.kts
include(":core:file-access")

// build.gradle.kts приложения
implementation(project(":core:file-access"))
```

Источник конфига:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class FileAccessConfigModule {
    @Binds
    @IntoSet
    abstract fun bindConfigSource(source: StoreFileAccessConfigSource): FileAccessConfigSource
}
```

`@IntoSet` нужен, чтобы источник был необязательным: `Optional` в Dagger требует API 24. Источников больше одного — `IllegalStateException`.

## Ограничения

Пароли пока приходят и сравниваются открытым текстом. Когда в конфиге появится хеш, поменяется только `Password`: фичи сравнивают пароль через `matches`.
