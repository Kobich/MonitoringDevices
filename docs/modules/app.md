# `:app`

Хост-приложение для проверки фичи на устройстве.

- `MonitoringDevicesApplication` создаёт `NearbyScanner` и хранит его.
- `MainActivity` содержит `NavHost`: стартовый экран с кнопкой и экран фичи, добавленный через `nearbyScanScreen(scanner)`.
