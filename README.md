# Алгоритмы

Нативное Android-приложение для создания, планирования и пошагового выполнения личных алгоритмов и чек-листов. Приложение работает локально, поддерживает ветвящиеся сценарии, медиа-заметки, таймеры, расписание и генерацию инструкций с помощью YandexGPT.

![Android](https://img.shields.io/badge/Android-SDK_36-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-2024.10-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material_3-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white)
![CameraX](https://img.shields.io/badge/CameraX-1.5-34A853?style=for-the-badge&logo=android&logoColor=white)
![YandexGPT](https://img.shields.io/badge/YandexGPT-API-FFCC00?style=for-the-badge&logo=yandexcloud&logoColor=black)
![Gradle](https://img.shields.io/badge/Gradle-9.6-02303A?style=for-the-badge&logo=gradle&logoColor=white)

## Возможности

- библиотека алгоритмов с поиском, избранным, дублированием и историей запусков;
- визуальный редактор последовательностей и ветвлений;
- информационные шаги, предупреждения, чек-листы, выбор вариантов, условия и таймеры;
- пошаговый runner с восстановлением активного запуска и расчётом прогресса по фактическому маршруту;
- фото, видеокружки, аудиозаметки и файлы, прикреплённые к шагам;
- фоновые таймеры и уведомления о важных этапах выполнения;
- календарное расписание с повторениями, напоминаниями и запуском связанного алгоритма;
- виджеты главного экрана для алгоритмов и расписания;
- генерация новых алгоритмов через YandexGPT;
- AI-улучшение существующих алгоритмов, метаданных и пунктов расписания с предпросмотром изменений;
- Markdown в описаниях шагов;
- настраиваемые навигация, отображение прогресса, поведение экрана и календаря.

## Как устроен алгоритм

Алгоритм состоит из шагов и переходов между ними. Обычный шаг ведёт к следующему, а условный выбирает ветку по ответу пользователя. Движок учитывает посещённые узлы, защищается от циклов и показывает диапазон прогресса, если точная длина маршрута ещё неизвестна.

Поддерживаемые типы шагов:

| Тип | Назначение |
| --- | --- |
| `Information` | Текстовая инструкция |
| `Warning` | Важное предупреждение |
| `Checkbox` | Действие или список подпунктов |
| `YesNo` | Выбор «да / нет» |
| `SingleChoice` | Один вариант из списка |
| `MultipleChoice` | Несколько вариантов |
| `Timer` | Шаг с обратным отсчётом |
| `Final` | Завершение сценария |

## YandexGPT

AI-функции используют Yandex Cloud Text Generation API. Пользователь вводит API-ключ сервисного аккаунта и ID каталога в настройках приложения. Сгенерированный результат всегда открывается как черновик: его можно проверить и отредактировать до сохранения.

Ответ модели проходит локальную проверку:

- обязательных полей и идентификаторов;
- целей переходов и достижимости шагов;
- корректности ветвлений;
- отсутствия недопустимых циклов;
- допустимых типов шагов и вариантов ответа.

Для реального использования рекомендуется отдельный сервисный аккаунт с минимальной ролью `yc.ai.languageModels.execute`, ограниченным сроком действия ключа и лимитом расходов.

## Безопасность и хранение данных

- алгоритмы, расписание, история и настройки хранятся локально;
- API-ключ и ID каталога не добавляются в исходники, ресурсы, `BuildConfig` или логи;
- реквизиты Yandex Cloud шифруются через `AES/GCM/NoPadding`;
- ключ шифрования создаётся в `AndroidKeyStore` и не экспортируется;
- зашифрованные данные сохраняются в Preferences DataStore;
- резервное копирование приложения отключено;
- реквизиты и запись Android Keystore можно полностью удалить из настроек.

Локальное хранение защищает ключ от случайной публикации, но не гарантирует безопасность на root-устройстве. Для production-сценария предпочтителен собственный backend-прокси.

## Технологии

| Область | Технологии |
| --- | --- |
| Язык и runtime | Kotlin 2.2, Java 17, Coroutines 1.9 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Состояние | Android ViewModel, Compose state |
| Локальные данные | Preferences DataStore, JSON-сериализация |
| Камера и медиа | CameraX, MediaRecorder, FileProvider |
| Фоновые задачи | Foreground services, AlarmManager, BroadcastReceiver |
| Интеграции | Yandex Cloud Text Generation API |
| Виджеты | Android App Widgets, RemoteViews |
| Сборка и тесты | Gradle 9.6.1, Android Gradle Plugin 9.2, JUnit 4 |

## Структура проекта

```text
app/src/main/java/ru/ritual/app/
├── data/               # локальные репозитории, активные запуски и статистика
│   ├── network/        # клиент YandexGPT
│   └── security/       # защищённое хранение реквизитов
├── domain/model/       # алгоритмы, граф переходов и расписание
├── media/              # фото, видео, аудио и файловые заметки
├── notification/       # уведомления, alarms и фон AI-генерации
├── timer/              # foreground service таймера
├── ui/
│   ├── components/     # переиспользуемые Compose-компоненты
│   ├── screens/        # главная, редактор, runner, AI, расписание и настройки
│   └── theme/          # тема приложения
├── widget/             # виджеты алгоритмов и расписания
└── MainActivity.kt
```

Основное направление зависимостей: `UI → domain ← data`. Экранное состояние и пользовательские действия координирует `AppViewModel`.

## Требования

- Android Studio с JDK 17;
- Android SDK 36;
- устройство или эмулятор с Android 8.0 (API 26) или новее.

## Сборка

```bash
git clone https://github.com/Sadsadhead/ritual-android.git
cd ritual-android
./gradlew :app:assembleDebug
```

Готовый APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

На macOS можно явно использовать JDK из Android Studio:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew :app:assembleDebug
```

## Тесты

```bash
./gradlew :app:testDebugUnitTest
```

Unit-тесты покрывают вычисление маршрутов графа и формирование повторяющихся событий расписания.

## Разрешения Android

Приложение запрашивает только разрешения, необходимые выбранным функциям:

- `INTERNET` — запросы к YandexGPT;
- `CAMERA` и `RECORD_AUDIO` — медиа-заметки;
- `POST_NOTIFICATIONS` — таймеры, активные алгоритмы и расписание;
- `SCHEDULE_EXACT_ALARM` — точные напоминания;
- `RECEIVE_BOOT_COMPLETED` — восстановление расписания после перезагрузки;
- foreground service — таймер и длительная AI-генерация.

Камера объявлена необязательной, поэтому приложение может устанавливаться на устройства без неё.
