# Внедрение файлового менеджера и просмотрщика через Hilt

Инструкция для приложения, которое уже использует Hilt и подключает три модуля: `:core:document`, `:features:file-manager` и `:features:document-viewer`.

## Что поставляется

| Модуль | Что это | Hilt |
|---|---|---|
| `:core:document:api` | контракт чтения файлов: `DocumentReader`, виды документов, ошибки | нет |
| `:core:document:impl` | реализация чтения: `DocumentCore(context).reader` | нет, `DocumentReader` в граф кладёт приложение |
| `:features:file-manager` | экран файлового менеджера | `FileManagerFeatureModule` |
| `:features:document-viewer` | экран просмотра | `DocumentViewerFeatureModule` |

Фичи зависят ещё от двух модулей, их нужно подключить вместе с ними или заменить своими с тем же API:

- `:core:common-ui` — тема `AppTheme`, общие компоненты и значки;
- `:core:logger:client:api` — логгер действий; сейчас это затычка, которая пишет в Logcat, настоящий клиент ставится на её место.

## Что каждый модуль кладёт в граф

| Hilt-модуль | Компонент | Что даёт | Что ждёт от приложения |
|---|---|---|---|
| `FileManagerFeatureModule` | `SingletonComponent` | `FileManagerFeatureApi`, `InternalFileSource` (корень `filesDir/storage`) | `@ApplicationContext` |
| `DocumentViewerFeatureModule` | `SingletonComponent` | `DocumentViewerFeatureApi` | `DocumentReader` |

ViewModel обеих фич помечены `@HiltViewModel` и создаются внутри фичи через `hiltViewModel()`. Приложению их трогать не нужно, но Activity, на которой показываются экраны, должна быть `@AndroidEntryPoint`.

`DocumentReader` в граф не кладёт ни один из модулей: `:core:document` сделан без DI-фреймворка. Его даёт приложение — шаг 5.

## Шаги

**1. Подключить модули.**

```kotlin
// settings.gradle.kts
include(":core:common-ui")
include(":core:logger:client:api")
include(":core:document:api")
include(":core:document:impl")
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

**6. Навигация.** Менеджер отдаёт выбранный файл, приложение открывает по нему просмотрщик. Каждый экран лучше держать в своём `composable(...)`: тогда ViewModel фичи живёт, пока экран в стеке, и освобождается при выходе с него.

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

Просмотрщик можно открыть и для файла не из менеджера: в `DocumentViewerScreen` передаётся любой `File`, который приложение может прочитать.

## Требования

- **minSdk приложения не ниже 23.** Офисные форматы открываются с Android 8 (API 26): этого требует библиотека OpenDocument.core, ниже они не поддерживаются. Модуль `:core:document:impl` разрешает её подключение при minSdk 23 через `tools:overrideLibrary`; приложению ничего для этого делать не нужно.
- **Jetpack Compose** в приложении: экраны фич — Compose-функции, их API использует `@Composable` и `Modifier`.
- **FileProvider.** Просмотрщик объявляет в своём манифесте провайдер `ViewerFileProvider` с authority `${applicationId}.documentviewer.files` для кнопки «Открыть в другом приложении». Свой класс выбран нарочно: с `FileProvider` приложения он не конфликтует. Конфликт будет, только если у приложения есть провайдер с таким же authority.
- **Хранилище.** Файлы менеджера лежат в `filesDir/storage`. Резервное копирование настраивает приложение: если документы не должны уходить в облачный бэкап, исключите `storage` в `data_extraction_rules.xml` или выключите бэкап совсем.

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
| `Manifest merger failed: ... uses-sdk:minSdkVersion` | minSdk приложения ниже 23 | поднять до 23 |
| `Manifest merger failed` с `android:authorities` | у приложения есть провайдер с authority `<applicationId>.documentviewer.files` | поменять authority у провайдера приложения |
