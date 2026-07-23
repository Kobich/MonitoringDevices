# Модуль `:scanner:radio:impl`

Модуль реализует одноразовое чтение `TelephonyManager.allCellInfo`. Он проверяет permissions, наличие cellular radio и SIM, фильтрует cells и нормализует signal strength.

## Работа scanner

Flow последовательно отправляет `Scanning` и итоговый `Cells`, после чего завершается. Для `REGISTERED_ONLY` остаются записи с `isRegistered=true`. Если modem не вернул cells, scanner публикует `Unavailable` с диагностической причиной.

## Файлы и классы

| Файл | Назначение |
|---|---|
| `AndroidRadioScanner.kt` | Читает `allCellInfo`, фильтрует и преобразует GSM/CDMA/WCDMA/LTE/NR данные. |
| `RadioScanDiagnostics.kt` | Проверяет наличие cellular radio и SIM, формирует причину пустого результата. |
| `RadioScannerModule.kt` | Регистрирует singleton `RadioScanner` в Koin. |
| `AndroidManifest.xml` | Объявляет location и phone state permissions. |

## Lifecycle и ограничения

Scanner построен на обычном `flow`, не регистрирует callbacks и не удерживает ресурсы после завершения. `allCellInfo` может вернуть cached или неполные данные; доступность соседних cells определяется модемом и оператором. Подробности находятся в [ограничениях платформы](../platform-limitations.md).
