package ru.ritual.app.domain.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class AlgorithmGraphTest {
    @Test
    fun progressShowsRangeAcrossShortAndLongBranches() {
        val checklist = checklist(
            step("start"),
            step("condition", StepType.YesNo, options = listOf("Да", "Нет")),
            step("short", parent = "condition", option = 0),
            step("long-1", parent = "condition", option = 1),
            step("long-2", parent = "condition", option = 1),
            step("long-3", parent = "condition", option = 1),
            step("final", StepType.Final),
        )

        assertEquals(
            AlgorithmProgressRange(
                completedSteps = 2,
                minTotalSteps = 4,
                maxTotalSteps = 6,
                minPercent = 33,
                maxPercent = 50,
            ),
            checklist.progressRange(1, listOf("start", "condition")),
        )
    }

    @Test
    fun progressBecomesExactAfterShortBranchIsSelected() {
        val checklist = checklist(
            step("start"),
            step("condition", StepType.YesNo, options = listOf("Да", "Нет")),
            step("short", parent = "condition", option = 0),
            step("long-1", parent = "condition", option = 1),
            step("long-2", parent = "condition", option = 1),
            step("final", StepType.Final),
        )

        assertEquals(
            AlgorithmProgressRange(3, 4, 4, 75, 75),
            checklist.progressRange(2, listOf("start", "condition", "short")),
        )
    }

    @Test
    fun improvementPreservesLocalMediaForStepsWithStableIds() {
        val attachment = StepAttachment("content://photo", AttachmentType.Photo, "Фото")
        val original = checklist(
            step("stable").copy(note = "Личная заметка", attachments = listOf(attachment)),
        )
        val proposal = GeneratedChecklist(
            title = "Улучшенный",
            description = "Точнее",
            category = "Работа",
            estimatedDurationMinutes = 5,
            symbol = "✨",
            steps = listOf(
                GeneratedStep(
                    id = "stable",
                    title = "Обновлённый этап",
                    description = "Новая формулировка",
                    type = "INFORMATION",
                    isRequired = true,
                    options = emptyList(),
                    defaultNextStepId = null,
                ),
            ),
        )

        val result = proposal.toChecklistReplacing(original)

        assertEquals("Личная заметка", result.steps.single().note)
        assertEquals(listOf(attachment), result.steps.single().attachments)
        assertEquals("Улучшенный", result.title)
    }

    private fun checklist(vararg steps: ChecklistStep) = Checklist(
        id = "test",
        title = "Тест",
        description = "",
        category = "Все",
        durationMinutes = 1,
        accent = Color.White,
        emoji = "✓",
        steps = steps.toList(),
    )

    private fun step(
        id: String,
        type: StepType = StepType.Information,
        options: List<String> = emptyList(),
        parent: String? = null,
        option: Int? = null,
    ) = ChecklistStep(
        id = id,
        eyebrow = "",
        title = id,
        description = "",
        type = type,
        options = options,
        parentConditionId = parent,
        parentOptionIndex = option,
    )
}
