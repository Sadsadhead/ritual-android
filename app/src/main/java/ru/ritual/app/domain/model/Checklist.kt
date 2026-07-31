package ru.ritual.app.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable

enum class StepType { Information, Checkbox, YesNo, SingleChoice, MultipleChoice, Timer, Final }

enum class AttachmentType { Photo, VideoCircle, Audio, File }

enum class GenerationCharacter(
    val title: String,
    val description: String,
    val instruction: String,
) {
    Balanced(
        "Сбалансированный",
        "Понятно, практично и без лишней формальности.",
        "Пиши ясно и практично, сохраняя нейтральный доброжелательный тон.",
    ),
    Official(
        "Официальный",
        "Строгие формулировки, однозначные требования и проверки.",
        "Используй официальный деловой стиль, точные формулировки и явные критерии выполнения.",
    ),
    Technical(
        "Технический",
        "Больше параметров, условий, измеримых значений и диагностики.",
        "Пиши как технический регламент: добавляй параметры, измеримые значения, проверки и диагностику ошибок.",
    ),
    Friendly(
        "Дружелюбный",
        "Мягкие объяснения, поддержка и простой разговорный язык.",
        "Пиши дружелюбно и спокойно, объясняй простыми словами и поддерживай пользователя.",
    ),
    Concise(
        "Лаконичный",
        "Минимум текста — только необходимые действия и решения.",
        "Будь предельно лаконичен: оставляй только необходимые действия, условия и предупреждения.",
    ),
    Creative(
        "Креативный",
        "Необычная подача и запоминающиеся названия без потери точности.",
        "Используй живую запоминающуюся подачу и образные названия, не жертвуя точностью и безопасностью.",
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
