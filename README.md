# Алгоритмы

Нативное Android-приложение для локальных пошаговых бытовых инструкций. Текущий MVP
показывает дизайн-систему, библиотеку демонстрационных чек-листов, поиск и фильтры,
пошаговый runner, таймер, историю, AI-форму, визуальный редактор с блок-схемой и
безопасный ввод реквизитов YandexGPT прямо на экране создания.

Редактор поддерживает создание блоков разных типов, открытие настроек блока нажатием
и перестановку долгим перетаскиванием. Доступны действие, информация, чек-лист,
условие, одиночный и множественный выбор, таймер, предупреждение, фото-проверка и финал.
К блокам можно прикреплять заметки, фото, видео, аудио и файлы.

AI-генерация использует Yandex Cloud Text Generation API и модель YandexGPT.
Сгенерированный алгоритм сначала открывается как редактируемый черновик. Каждый шаг
поддерживает описание, личную заметку, фото, видеокружок, аудио и файл.

## Безопасность API-ключа

- API-ключ сервисного аккаунта и ID каталога вводятся только в приложении;
- реквизиты шифруются `AES/GCM/NoPadding`;
- ключ шифрования создаётся в `AndroidKeyStore` и не экспортируется;
- зашифрованная строка хранится в Preferences DataStore;
- ключ не попадает в исходники, ресурсы, `BuildConfig`, логи или экспорт;
- backup приложения отключён, DataStore исключён из device transfer;
- в настройках есть полное удаление реквизитов и Keystore entry.

Для production рекомендуется небольшой backend. Локальный режим следует использовать
с отдельным API-ключом сервисного аккаунта Yandex Cloud, ограниченным областью
`yc.ai.languageModels.execute`, сроком действия и лимитом расходов.

## Структура

```text
app/src/main/java/ru/ritual/app/
├── data/
│   ├── ChecklistRepository.kt
│   └── security/SecureApiKeyStore.kt
├── domain/model/Checklist.kt
├── ui/
│   ├── components/ChecklistCard.kt
│   ├── screens/
│   ├── theme/Theme.kt
│   ├── AppViewModel.kt
│   └── RitualApp.kt
└── MainActivity.kt
```

Направление зависимостей: `presentation → domain ← data`. В дальнейшей декомпозиции
эти границы переводятся в модули `core:model`, `core:domain`, `core:data`, `core:ui`,
`core:database`, `core:network`, а экраны — в `feature:*`.

## Навигация

Нижний уровень: `Главная → История → Создать с ИИ → Настройки`.
Runner открывается полноэкранно через `runner/{checklistId}`.

## Контракт движка выполнения

```kotlin
interface ChecklistExecutionEngine {
    fun validate(graph: ChecklistGraph): ValidationReport
    fun start(graph: ChecklistGraph): ExecutionState
    fun submit(state: ExecutionState, answer: StepAnswer): TransitionResult
    fun goBack(state: ExecutionState): TransitionResult
    fun calculateProgress(state: ExecutionState): Float
}
```

Движок должен сортировать правила по приоритету, вычислять AND/OR-группы, защищаться
от циклов, отбрасывать ответы недоступной ветки и считать прогресс по рассчитанному
маршруту, а не по общему числу узлов.

## JSON для AI

```json
{
  "title": "Название",
  "description": "Описание",
  "category": "Дом",
  "tags": ["быстро"],
  "estimatedDurationMinutes": 10,
  "startStepId": "step_1",
  "steps": [
    {
      "id": "step_1",
      "title": "Первый шаг",
      "description": "Что сделать",
      "type": "CHECKBOX",
      "isRequired": true,
      "options": [],
      "checklistItems": ["Первый подпункт", "Второй подпункт"],
      "defaultNextStepId": "step_2"
    }
  ],
  "transitions": [],
  "warnings": []
}
```

Ответ AI проходит десериализацию, проверку идентификаторов, целей переходов,
достижимости и циклов. Сохранение допускается только после предпросмотра пользователем.
Условные шаги переходят дальше сразу после выбора варианта. Обычный шаг может содержать
собственный список подпунктов, которые отмечаются независимо во время выполнения.
Запущенный таймер работает через foreground service после сворачивания приложения.
Системные уведомления и звуковые сигналы срабатывают за 5 минут, за 1 минуту и по
окончании отсчёта; Android 13+ запрашивает разрешение на уведомления при первом запуске.

## План развития

1. Room, редактор и локальная история.
2. Типы ответов, граф переходов и unit-тесты движка.
3. CameraX, аудио/видео и файловое хранилище.
4. YandexGPT Text Generation API, JSON-ответ, валидация и предпросмотр.
5. SAF-импорт/экспорт с checksum и защитой от path traversal.

Ключевые риски: сложность миграций графа, восстановление динамического runner,
очистка общих медиа, ограничения фоновых таймеров Android и компрометация ключа
на root-устройстве.

## Сборка

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
