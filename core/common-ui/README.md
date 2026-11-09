# Общий UI

## Описание

Тема, значки и Compose-компоненты, которые используют все фичи, чтобы экраны выглядели одинаково и не собирали Material-компоненты каждый по-своему. Компонент переносится сюда, как только он нужен второй фиче. Бизнес-логики в модуле нет.

## Архитектура и стек

```
core/common-ui/
├── src/main/kotlin/com/android/core/commonui/
│   ├── theme/        # MonitoringDevicesTheme, StatusColors, типографика
│   ├── icons/        # AppIcons, FileKind, FileTypeIcons / FileTypeIcon
│   ├── components/   # по компоненту на файл, превью в том же файле
│   ├── state/        # rememberDelayedVisibility
│   └── catalog/      # UiCatalogScreen, DialogShowcaseScreen — витрина для разработчиков
└── src/main/res/values/strings.xml   # строки с префиксом common_ui_
```

* **Слой:** Presentation.
* **Ключевые технологии:** Jetpack Compose, Material 3. Compose и Material 3 подключены как `api`: публичные компоненты принимают их типы.
* **Зависимости:** не зависит от других модулей проекта.

Минимальная версия SDK: 23. Версия компиляции SDK: 36.

Правила модуля:
- цвета только из `MaterialTheme.colorScheme` и `MonitoringDevicesTheme.statusColors` (кроме цветных значков файлов);
- строки в `res/values/strings.xml` с префиксом `common_ui_`;
- фичи берут значки только из `AppIcons`, а не из `Icons.*` и не из своих drawable;
- компоненты без состояния: данные приходят параметрами, события уходят колбэками; таймеры, переходы и ViewModel — в фичах;
- один публичный компонент — один файл с его именем; имена по назначению, без префикса `App` (кроме `AppIcons`);
- новый компонент или значок добавляется в `UiCatalogScreen`, новое окно — в `DialogShowcaseScreen`.

## Подключение модуля к проекту

```kotlin
// settings.gradle.kts
include(":core:common-ui")

// build.gradle.kts фичи
implementation(project(":core:common-ui"))
```

## Описание API

### Тема и значки

| API | Описание |
|---|---|
| `MonitoringDevicesTheme(darkTheme, dynamicColor) { … }` | Цвета устройства (Material You на Android 12+, базовая палитра ниже), светлая и тёмная по системе. `dynamicColor = false` — стабильные цвета для превью |
| `MonitoringDevicesTheme.statusColors` | Цвета статусов, которых нет в Material: `positive` (ок), `pending` (в процессе), `negative` (проблема). В каждом мягкая пара `accent` (значок на фоне экрана) + `container` / `onContainer` (плашка, карточка); свои для светлой и тёмной темы |
| `AppIcons.Back`, `Close`, `Delete`, `More`, `Add`, `Check`, `Play`, `Pause`, `Folder`, `File`, `Link`, `LinkOff` | Монохромные значки; цвет задаёт `Icon(tint = …)` |
| `FileKind.of(fileName, isDirectory)` | Тип файла по расширению: `FOLDER`, `PDF`, `DOCUMENT`, `SPREADSHEET`, `PRESENTATION`, `TEXT`, `IMAGE`, `AUDIO`, `VIDEO`, `ARCHIVE`, `OTHER` |
| `FileTypeIcon(kind, size)` | Цветной значок файла; рисуется как `Image`, не через `Icon` с `tint` |

### Компоненты

| API | Описание |
|---|---|
| `BackTopAppBar(title, onNavigationClick, actions)` | Верхняя панель с кнопкой «Назад» |
| `CenteredContent { … }`, `LoadingContent(text)`, `MessageContent(title, text, actionLabel, onActionClick)` | Состояния экрана: по центру, загрузка, пусто или ошибка |
| `Spinner(compact)` | Круглый индикатор загрузки: обычный для пустой области, `compact` — внутри контента |
| `ProgressBar(progress, label, showPercent)` | Линейный прогресс; `progress = null` — неизвестен |
| `ConfirmDialog(title, confirmLabel, onConfirm, onDismiss, dismissLabel, destructive, dismissible) { тело }` | Диалог с одной или двумя кнопками и любым телом: текст, поле ввода, прогресс |
| `ChoiceDialog(ChoiceDialogUi(title, choices, text), onChoice, onDismiss)` | Диалог-данные с любым числом вариантов `DialogChoice(value, label, destructive)`; основа цепочки окон |
| `SelectionIndicator(selected)` | Круглая отметка выбора в списке |
| `ConnectionStatusIndicator(status)`, `ConnectionStatusIcon(status)` | Статус подключения: `CONNECTED` — мягкий зелёный и ссылка, `CONNECTING` — янтарный и спиннер, `DISCONNECTED` — приглушённый красный и перечёркнутая ссылка. Без цвета статус различим по значку |
| `ConnectionStatusTile(status, label, iconSize)` | Крупный статус: значок в мягком цветном круге и подпись под ним — для карточки устройства или пустой области экрана |
| `rememberDelayedVisibility(visible)` | `true`, только если `visible` держится дольше задержки: индикатор не мигает на быстрых операциях |
| `UiCatalogScreen(onBackClick)`, `DialogShowcaseScreen(onBackClick)` | Все компоненты, значки и окна на двух экранах |

### Цепочка окон

Окно — это состояние, а не вызов. ViewModel фичи держит текущий шаг, экран показывает его, ответ приходит в ViewModel, и она кладёт в состояние следующий шаг или `null`. Решение «что дальше» живёт в фиче, модуль даёт только окна.

## Примеры использования

```kotlin
// ViewModel
var dialog by mutableStateOf<ChoiceDialogUi<KeepCopy>?>(null)
    private set

fun onDeleteConfirmed() {
    dialog = ChoiceDialogUi(
        title = "Keep a copy?",
        choices = listOf(
            DialogChoice(KeepCopy.YES, "Keep a copy"),
            DialogChoice(KeepCopy.NO, "Delete for good", destructive = true)
        )
    )
}

fun onKeepCopyChoice(choice: KeepCopy) {
    delete(keepCopy = choice == KeepCopy.YES)
    dialog = null
}

fun onDialogDismiss() {
    dialog = null
}

// Экран
viewModel.dialog?.let { dialog ->
    ChoiceDialog(dialog = dialog, onChoice = viewModel::onKeepCopyChoice, onDismiss = viewModel::onDialogDismiss)
}
```
