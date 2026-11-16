# Внедрение файлового менеджера и просмотрщика через Hilt

Инструкция для приложения, которое уже использует Hilt и подключает модули `:core:document`, `:features:file-manager` и `:features:document-viewer`, а с ними `:core:file-access` — конфиг администратора, которому подчиняются обе фичи.

## Что поставляется

| Модуль | Что это | Hilt |
|---|---|---|
| `:core:document:api` | контракт чтения файлов: `DocumentReader`, виды документов, ошибки | нет |
| `:core:document:impl` | реализация чтения: `DocumentCore(context).reader` | нет, `DocumentReader` в граф кладёт приложение |
| `:core:file-access` | конфиг администратора: что доступно и с каким паролем | `FileAccessModule` |
| `:features:file-manager` | экран файлового менеджера | `FileManagerFeatureModule` |
| `:features:document-viewer` | экран просмотра | `DocumentViewerFeatureModule` |

Фичи зависят ещё от двух модулей, их нужно подключить вместе с ними или заменить своими с тем же API:

- `:core:common-ui` — тема `AppTheme`, общие компоненты и значки;
- `:core:logger:client:api` — логгер действий; сейчас это затычка, которая пишет в Logcat, настоящий клиент ставится на её место.

## Что каждый модуль кладёт в граф

| Hilt-модуль | Компонент | Что даёт | Что ждёт от приложения |
|---|---|---|---|
| `FileAccessModule` | `SingletonComponent` | `FileAccessRules`, пустой набор источников конфига | источник конфига, необязательно (шаг 6) |
| `FileManagerFeatureModule` | `SingletonComponent` | `FileManagerFeatureApi` | `@ApplicationContext` |
| `DocumentViewerFeatureModule` | `SingletonComponent` | `DocumentViewerFeatureApi` | `DocumentReader` |

ViewModel обеих фич помечены `@HiltViewModel` и создаются внутри фичи через `hiltViewModel()` с assisted-фабрикой: у менеджера своя ViewModel на каждое хранилище, у просмотрщика — на каждый файл. Приложению их трогать не нужно, но Activity, на которой показываются экраны, должна быть `@AndroidEntryPoint`.

`DocumentReader` в граф не кладёт ни один из модулей: `:core:document` сделан без DI-фреймворка. Его даёт приложение — шаг 5.

## Шаги

**1. Подключить модули.**

```kotlin
// settings.gradle.kts
include(":core:common-ui")
include(":core:logger:client:api")
include(":core:document:api")
include(":core:document:impl")
include(":core:file-access")
include(":features:file-manager")
include(":features:document-viewer")
```

**2. Gradle приложения.** Плагины Hilt и KSP, фичи, реализация чтения и сам Hilt. Модули фич подключают Hilt у себя сами, приложению нужно только своё.

```kotlin
// build.gradle.kts приложения
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":features:file-manager"))
    implementation(project(":features:document-viewer"))
    implementation(project(":core:document:impl"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

Версии, с которыми модули собраны (`gradle/libs.versions.toml`): Hilt 2.60.1, `hilt-navigation-compose` 1.2.0, KSP 2.3.13. Берите ту же версию Hilt, что в модулях, или обновляйте её сразу везде: Gradle оставит одну, старшую, а Hilt-модули фич должны быть собраны компилятором той же версии.

**3. Application.**

```kotlin
@HiltAndroidApp
class App : Application()
```

**4. Activity.** Экраны фич — Compose-функции. Их показывает Activity с `@AndroidEntryPoint`, а API фич приходят в неё через `@Inject`.

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var fileManager: FileManagerFeatureApi
    @Inject lateinit var documentViewer: DocumentViewerFeatureApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { AppNavigation(fileManager, documentViewer) } }
    }
}
```

**5. `DocumentReader` для просмотрщика.** Один на всё приложение: все чтения делят кэш и очередь конвертации.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DocumentCoreModule {
    @Provides
    @Singleton
    fun provideDocumentReader(@ApplicationContext context: Context): DocumentReader = DocumentCore(context).reader
}
```

**6. Конфиг администратора.** Обе фичи берут его из `FileAccessRules` и применяют сами, в том числе когда он меняется на открытом экране. Приложение только отдаёт источник: реализует `FileAccessConfigSource` и кладёт его в набор. Без источника конфига нет, доступно всё.

```kotlin
class StoreFileAccessConfigSource @Inject constructor(store: Store) : FileAccessConfigSource {
    // JSON из store — FileAccessConfigJson.parse; null, ошибка в потоке или битый JSON — «конфига нет»
    override val config: Flow<FileAccessConfig?> = store.fileManagerConfigJson.map { json ->
        json?.let(FileAccessConfigJson::parse)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FileAccessConfigModule {
    @Binds
    @IntoSet
    abstract fun bindConfigSource(source: StoreFileAccessConfigSource): FileAccessConfigSource
}
```

Источник должен сразу отдавать значение (например, `StateFlow` или `DataStore.data`): экраны ждут первое значение, прежде чем показать закрытый файл или меню. `@IntoSet`, а не обычный `@Binds`, нужен затем, чтобы источник был необязательным; `Optional` в Dagger требует API 24. Формат конфига — в [WIKI менеджера](../features/file-manager/WIKI.md#конфиг-администратора).

**7. Навигация.** Менеджер отдаёт выбранный файл, приложение открывает по нему просмотрщик. Каждый экран лучше держать в своём `composable(...)`: тогда ViewModel фичи живёт, пока экран в стеке, и освобождается при выходе с него.

```kotlin
NavHost(navController, startDestination = "file_manager") {
    composable("file_manager") {
        fileManager.FileManagerScreen(
            modifier = Modifier,
            onBackClick = { navController.popBackStack() },
            onFileOpen = { file -> navController.navigate("file_viewer?path=${Uri.encode(file.absolutePath)}") }
        )
    }
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
}
```

Чтобы показать хранилище другой фичи, передайте его первым параметром: `fileManager.FileManagerScreen(storage = FileStorage.named("reports"), ...)`. Менеджер покажет только папку `filesDir/storage/reports`.

Просмотрщик можно открыть и для файла не из менеджера: в `DocumentViewerScreen` передаётся любой `File`, который приложение может прочитать.

## Требования

- **minSdk приложения не ниже 23.** Офисные форматы открываются с Android 8 (API 26): этого требует библиотека OpenDocument.core, ниже они не поддерживаются. Модуль `:core:document:impl` разрешает её подключение при minSdk 23 через `tools:overrideLibrary`; приложению ничего для этого делать не нужно.
- **Jetpack Compose** в приложении: экраны фич — Compose-функции, их API использует `@Composable` и `Modifier`.
- **FileProvider.** Просмотрщик объявляет в своём манифесте провайдер `ViewerFileProvider` с authority `${applicationId}.documentviewer.files` для кнопки «Открыть в другом приложении». Свой класс выбран нарочно: с `FileProvider` приложения он не конфликтует. Конфликт будет, только если у приложения есть провайдер с таким же authority.
- **Хранилище.** Файлы всех хранилищ лежат в `filesDir/storage/<имя>`, собственные файлы менеджера — в `filesDir/storage/file-manager`. Резервное копирование настраивает приложение: если документы не должны уходить в облачный бэкап, исключите `storage` в `data_extraction_rules.xml` или выключите бэкап совсем.

## Проверка

1. Приложение собирается, Hilt не выдаёт ошибок графа.
2. Экран менеджера открывается, импорт файла через меню ⋮ кладёт его в список.
3. Нажатие на docx открывает просмотрщик, документ показывается.

Тестовые файлы всех поддерживаемых форматов — в `docs/samples/formats`, их можно импортировать папкой целиком.

## Частые ошибки

| Ошибка | Причина | Что сделать |
|---|---|---|
| `[Dagger/MissingBinding] DocumentReader cannot be provided` | нет шага 5 | добавить `DocumentCoreModule` |
| `Hilt Activity must be attached to an @HiltAndroidApp Application` | нет `@HiltAndroidApp` | шаг 3 |
| `Given component holder class MainActivity does not implement interface GeneratedComponent` или падение в `hiltViewModel()` | Activity без `@AndroidEntryPoint` | шаг 4 |
| `[Dagger/DuplicateBindings] FileManagerFeatureApi is bound multiple times` | приложение само связывает API фичи | убрать свою привязку: её делает модуль фичи |
| `IllegalStateException: More than one FileAccessConfigSource is bound` | в набор положено несколько источников | оставить один |
| `Manifest merger failed: ... uses-sdk:minSdkVersion` | minSdk приложения ниже 23 | поднять до 23 |
| `Manifest merger failed` с `android:authorities` | у приложения есть провайдер с authority `<applicationId>.documentviewer.files` | поменять authority у провайдера приложения |
