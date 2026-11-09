# MonitoringDevices

Android-проект с фичей сканирования ближайших устройств и приложением-хостом для её проверки.

| Модуль | Описание |
|---|---|
| [`:scanner`](scanner/README.md) | Фича: сканеры Wi-Fi и Bluetooth и их Compose-экраны. |
| `:app` | Пример хоста: стартовый экран, из которого фича открывается через Compose Navigation. |

Подключение фичи в приложении-хосте:

```kotlin
NavHost(navController = navController, startDestination = "home") {
    composable("home") { HomeScreen(onOpenScannerClick = { navController.navigateToNearbyScan() }) }
    nearbyScanScreen(scanner = NearbyScanner(applicationContext))
}
```

Сборка и тесты:

```bash
./gradlew :app:assembleDebug :scanner:testDebugUnitTest
```

Подробные заметки по устройству проекта — в [docs/](docs/README.md).
