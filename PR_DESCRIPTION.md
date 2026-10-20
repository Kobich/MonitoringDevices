## Описание

Фича сканирования ближайших Wi-Fi сетей и Bluetooth-устройств вынесена в самостоятельный модуль `:scanner`.

- `:scanner`: API сканеров (`WifiScannerApi`, `BluetoothScannerApi`, общий `NearbyScanner`), реализации и Compose-экраны.
- Подключение к хосту через Compose Navigation: `nearbyScanScreen(scanner)` и `navigateToNearbyScan()`.
- Сканирование идёт только на открытом экране. Foreground service, фоновая работа, Radio и Bluetooth Classic удалены.
- Bluetooth: LE-скан (находит и LE-, и dual-устройства), сглаживание RSSI, пометка устаревших устройств через 30 с и удаление через 2 мин, сырые manufacturer data и service UUID.
- Wi-Fi: интервал скана зависит от scan throttling.
- Только Android 16 (`minSdk` 36), манифест упрощён.
- Документация: `scanner/README.md`.

## Как тестировать

Unit-тесты: `./gradlew :scanner:testDebugUnitTest`

На устройстве с Android 16:
1. Открыть приложение, нажать «Open nearby scanner».
2. Wi-Fi: нажать Start, выдать разрешения, дождаться списка сетей. Открыть сеть и проверить детали. Если в настройках разработчика включён scan throttling, должен появиться диалог.
3. Bluetooth: нажать Start, выдать разрешения, проверить список. Устройства без имени показываются как «Unknown device». Проверить сортировку и детали устройства (manufacturer data).
4. Выключить устройство или отойти от него: через 30 с карточка становится полупрозрачной, через 2 мин пропадает.
5. Свернуть приложение: скан останавливается. Вернуться: скан продолжается. Выйти назад с экрана фичи: скан останавливается.
6. Выключить Wi-Fi, Bluetooth или геолокацию и нажать Start: должна появиться понятная причина вместо пустого списка.
7. Отказать в разрешениях: статус «Permissions denied».

## Известные проблемы

- `LifecycleResumeEffect` и `androidx.lifecycle.compose.LocalLifecycleOwner` требуют Lifecycle 2.8+. В проекте указан 2.6.1, нужная версия приходит транзитивно через Compose BOM.
