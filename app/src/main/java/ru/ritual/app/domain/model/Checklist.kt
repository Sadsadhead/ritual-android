package ru.ritual.app.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable

enum class StepType { Information, Warning, Checkbox, YesNo, SingleChoice, MultipleChoice, Timer, Final }

enum class AttachmentType { Photo, VideoCircle, Audio, File }

enum class GenerationCharacter(
    val symbol: String,
    val title: String,
    val description: String,
    val instruction: String,
) {
    Balanced(
        "◎",
        "Навигатор",
        "Спокойный баланс деталей, проверок и ясных действий.",
        "Пиши как опытный навигатор: ясно, практично и последовательно. Балансируй краткость, полезные проверки и объяснения.",
    ),
    Official(
        "▤",
        "Регламент",
        "Строго, однозначно, с критериями готовности.",
        "Создай строгий регламент: точные формулировки, явные критерии выполнения, контрольные точки и минимум двусмысленности.",
    ),
    Technical(
        "⌘",
        "Инженер",
        "Параметры, диагностика, условия и обработка ошибок.",
        "Проектируй как инженер: добавляй измеримые параметры, зависимости, диагностику ошибок, предупреждения и безопасные варианты восстановления.",
    ),
    Friendly(
        "☀",
        "Наставник",
        "Мягко объясняет и помогает не потерять уверенность.",
        "Пиши как заботливый наставник: простыми словами, без давления, с короткими подсказками и поддержкой в сложных местах.",
    ),
    Concise(
        "⚡",
        "Спринт",
        "Самый короткий безопасный путь к результату.",
        "Оптимизируй под скорость: только необходимые действия, решения и критические предупреждения; убирай повторы и лишние пояснения.",
    ),
    Creative(
        "✦",
        "Сценарист",
        "Живая подача и запоминающийся маршрут без потери точности.",
        "Построй запоминающийся сценарий с живыми названиями и уместными образами, но сохрани точность, безопасность и реальную логику ветвлений.",
    ),
}

@Immutable
data class StepAttachment(
    val uri: String,
    val type: AttachmentType,
    val name: String,
)

@Immutable
data class ChecklistStep(
    val id: String,
    val eyebrow: String,
    val title: String,
    val description: String,
    val type: StepType = StepType.Checkbox,
    val timerSeconds: Int? = null,
    val checklistItems: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val note: String = "",
    val attachments: List<StepAttachment> = emptyList(),
    val parentConditionId: String? = null,
    val parentOptionIndex: Int? = null,
)

@Immutable
data class Checklist(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val durationMinutes: Int,
    val accent: Color,
    val emoji: String,
    val steps: List<ChecklistStep>,
    val isFavorite: Boolean = false,
    val progress: Float = 0f,
    val tags: List<String> = emptyList(),
)

@Immutable
data class AlgorithmSuggestion(
    val checklist: Checklist,
    val reason: String,
)

@Immutable
data class ActiveAlgorithmRun(
    val algorithmId: String,
    val title: String,
    val emoji: String,
    val accentArgb: Int,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val startedAtMillis: Long,
    val completedSteps: Int = 1,
    val minTotalSteps: Int = totalSteps,
    val maxTotalSteps: Int = totalSteps,
    val minPercent: Int = 0,
    val maxPercent: Int = 0,
    val visitedStepIds: List<String> = emptyList(),
)

enum class MetadataTarget { Title, Description, Classification }

@Immutable
data class AlgorithmMetadataSuggestion(
    val title: String,
    val description: String,
    val category: String,
    val tags: List<String>,
)

@Immutable
data class AppPreferences(
    val tapNavigation: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val autoPlayVideoNotes: Boolean = false,
    val generationNotifications: Boolean = true,
    val calendarWeekStartsMonday: Boolean = true,
    val calendarDefaultView: ScheduleViewMode = ScheduleViewMode.Month,
    val calendarShowNotes: Boolean = true,
    val calendarOfferSystemExport: Boolean = true,
    val showActiveRunOnHome: Boolean = true,
    val showProgressRange: Boolean = true,
    val confirmBeforeStopping: Boolean = true,
    val compactAlgorithmCards: Boolean = true,
    val calendarShowWeekNumbers: Boolean = true,
    val calendarHighlightCurrentWeek: Boolean = true,
)

@Immutable
data class RunRecord(
    val title: String,
    val finishedAt: String,
    val duration: String,
    val percent: Int,
)

@Immutable
data class GeneratedStep(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val isRequired: Boolean,
    val options: List<String>,
    val defaultNextStepId: String?,
    val checklistItems: List<String> = emptyList(),
    val timerSeconds: Int? = null,
    val optionNextStepIds: List<String?> = emptyList(),
)

@Immutable
data class GeneratedChecklist(
    val title: String,
    val description: String,
    val category: String,
    val estimatedDurationMinutes: Int,
    val steps: List<GeneratedStep>,
    val accentArgb: Int? = null,
    val symbol: String = "◇",
    val tags: List<String> = emptyList(),
)
