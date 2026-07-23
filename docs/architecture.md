# Архитектура MonitoringDevices

## Назначение

Приложение показывает беспроводное окружение Android-устройства в трёх независимых разделах: Wi-Fi, Bluetooth и Radio. Пользовательские экраны находятся в `feature:*`, работа с Android API изолирована в `scanner:*`, а продолжительное Wi-Fi/Bluetooth-сканирование координирует `service:monitoring`.

## Слои

```text
app
  -> feature:*:impl
  -> scanner:*:impl
  -> service:monitoring:impl

feature:wifi:impl ---------> service:monitoring:api -> scanner:wifi:api
feature:bluetooth:impl ----> service:monitoring:api -> scanner:bluetooth:api
feature:radio:impl ---------------------------------> scanner:radio:api

scanner:*:impl -> scanner:*:api
service:monitoring:impl -> service:monitoring:api
```

`api`-модуль содержит внешний контракт и переносимые модели. `impl`-модуль содержит Android-зависимую реализацию, UI или инфраструктуру. Такое разделение не позволяет `app` и соседним фичам зависеть от внутренних классов реализации.

## Пользовательские фичи

Каждая фича публикует один Compose-контракт через `feature:*:api`. Реализация строится по однонаправленной схеме:

```text
FeatureApi -> Route -> Screen
                  -> ViewModel -> UiState
                  -> пользовательское действие -> ViewModel
```

Wi-Fi и Bluetooth запускают мониторинг через `MonitoringController`. Их ViewModel подписываются на события controller, но не владеют scanner job. Поэтому уничтожение экрана или переход на другую вкладку не должно само по себе останавливать уже запущенный мониторинг. Radio является коротким запросом текущего состояния модема и работает напрямую через `RadioScanner`.

## Scanner-слой

Scanner начинает работу при collection возвращённого `Flow`. Внешний код получает типизированные события: запуск, данные, отсутствие разрешений, недоступность capability или ошибку платформы.

- `scanner:wifi` читает cache Wi-Fi и при активном режиме вызывает `WifiManager.startScan()`.
- `scanner:bluetooth` объединяет BLE callback и Bluetooth Classic discovery в общую модель устройств.
- `scanner:radio` один раз читает `TelephonyManager.allCellInfo` и завершает Flow.

Scanner не знает об экранах, навигации и foreground service. Освобождение receiver и Bluetooth callback привязано к отмене collection.

## Foreground monitoring

`service:monitoring` обслуживает только Wi-Fi и Bluetooth. Один `DeviceMonitoringService` содержит независимые jobs для активных типов:

```text
WIFI                 -> Wi-Fi job
BLUETOOTH            -> Bluetooth job
WIFI + BLUETOOTH     -> две независимые jobs
пустой набор         -> остановка service
```

`DefaultMonitoringController` хранит активные типы, последние события и количество результатов. Активные типы сохраняются в `SharedPreferences`. `DeviceMonitoringService` наблюдает это состояние, синхронизирует jobs и обновляет постоянное уведомление. Wi-Fi использует foreground service type `location`, Bluetooth — `connectedDevice`.

## Движение данных

```text
Start в UI
  -> Interactor
  -> MonitoringController.startMonitoring(type)
  -> DeviceMonitoringService
  -> Scanner.scan()
  -> Scanner event
  -> MonitoringController
  -> Interactor
  -> ViewModel / reducer
  -> UiState
  -> Screen
```

Stop удаляет только выбранный тип. Сервис завершается после удаления последнего типа. Действие `Stop all` в уведомлении очищает оба типа.

## Границы ответственности

- `app` собирает DI-граф, тему и навигацию.
- `feature:*:api` задаёт внешний Compose-контракт.
- `feature:*:impl` владеет пользовательским состоянием и отображением.
- `scanner:*:api` задаёт scanner-контракт, события и модели результатов.
- `scanner:*:impl` работает с Android framework API и разрешениями.
- `service:monitoring:api` связывает Wi-Fi/Bluetooth фичи с долгоживущим мониторингом.
- `service:monitoring:impl` владеет foreground service, уведомлением и scan jobs.

Детали классов находятся на [страницах модулей](README.md#модули). Платформенное поведение, которое архитектура не может отменить, вынесено в [ограничения](platform-limitations.md).
