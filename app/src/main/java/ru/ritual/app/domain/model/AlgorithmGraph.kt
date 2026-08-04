package ru.ritual.app.domain.model

import androidx.compose.ui.graphics.Color

data class AlgorithmProgressRange(
    val completedSteps: Int,
    val minTotalSteps: Int,
    val maxTotalSteps: Int,
    val minPercent: Int,
    val maxPercent: Int,
)

fun Checklist.progressRange(currentStepIndex: Int, visitedStepIds: List<String>): AlgorithmProgressRange {
    if (steps.isEmpty()) return AlgorithmProgressRange(0, 0, 0, 0, 0)
    val safeIndex = currentStepIndex.coerceIn(steps.indices)
    val completed = visitedStepIds.size.coerceAtLeast(1)
    val remaining = steps.pathLengthRangeFrom(safeIndex)
    val minTotal = (completed - 1 + remaining.first).coerceAtLeast(completed)
    val maxTotal = (completed - 1 + remaining.last).coerceAtLeast(minTotal)
    val minPercent = (completed * 100 / maxTotal).coerceIn(0, 100)
    val maxPercent = (completed * 100 / minTotal).coerceIn(minPercent, 100)
    return AlgorithmProgressRange(completed, minTotal, maxTotal, minPercent, maxPercent)
}

fun List<ChecklistStep>.firstStepInBranchIndex(conditionId: String, optionIndex: Int): Int? =
    indices.firstOrNull { index ->
        this[index].parentConditionId == conditionId &&
            (this[index].parentOptionIndex ?: 0) == optionIndex
    }

fun List<ChecklistStep>.nextStepIndexAfter(index: Int, visited: Set<Int> = emptySet()): Int? {
    if (index !in indices || index in visited) return null
    val current = this[index]
    val sibling = indices.firstOrNull { candidateIndex ->
        candidateIndex > index &&
            this[candidateIndex].parentConditionId == current.parentConditionId &&
            (this[candidateIndex].parentOptionIndex ?: 0) == (current.parentOptionIndex ?: 0)
    }
    if (sibling != null) return sibling
    val parentIndex = current.parentConditionId?.let { conditionId -> indexOfFirst { it.id == conditionId } }
        ?.takeIf { it >= 0 }
        ?: return null
    return nextStepIndexAfter(parentIndex, visited + index)
}

private fun List<ChecklistStep>.pathLengthRangeFrom(start: Int): IntRange {
    val memo = mutableMapOf<Int, IntRange>()
    fun visit(index: Int, visiting: Set<Int>): IntRange {
        if (index !in indices || index in visiting) return 0..0
        memo[index]?.let { return it }
        val step = this[index]
        if (step.type == StepType.Final) return 1..1
        val successors = if (step.type == StepType.YesNo || step.type == StepType.SingleChoice) {
            val optionCount = step.options.size.coerceAtLeast(2)
            (0 until optionCount).mapNotNull { optionIndex ->
                firstStepInBranchIndex(step.id, optionIndex) ?: nextStepIndexAfter(index)
            }.distinct()
        } else {
            listOfNotNull(nextStepIndexAfter(index))
        }
        if (successors.isEmpty()) return 1..1
        val ranges = successors.map { visit(it, visiting + index) }
        val result = (1 + ranges.minOf { it.first })..(1 + ranges.maxOf { it.last })
        memo[index] = result
        return result
    }
    return visit(start, emptySet())
}

fun GeneratedChecklist.inferBranchPlacements(): Map<String, Pair<String, Int>> {
    val byId = steps.associateBy { it.id }
    val placements = mutableMapOf<String, Pair<String, Int>>()
    steps.filter { it.type == "YES_NO" || it.type == "SINGLE_CHOICE" }.forEach { condition ->
        val optionCount = condition.options.size.coerceAtLeast(2)
        val paths = (0 until optionCount).map { optionIndex ->
            val firstId = condition.optionNextStepIds.getOrNull(optionIndex)
                ?: condition.defaultNextStepId.takeIf { optionIndex == 0 }
            buildSet {
                var currentId = firstId
                while (currentId != null && currentId != condition.id && add(currentId)) {
                    currentId = byId[currentId]?.defaultNextStepId
                }
            }
        }
        paths.forEachIndexed { optionIndex, path ->
            path.filter { targetId -> paths.count { targetId in it } == 1 }
                .forEach { targetId -> placements.putIfAbsent(targetId, condition.id to optionIndex) }
        }
    }
    return placements
}

fun GeneratedChecklist.toChecklistReplacing(original: Checklist): Checklist {
    val placements = inferBranchPlacements()
    val originals = original.steps.associateBy(ChecklistStep::id)
    return original.copy(
        title = title,
        description = description,
        category = category,
        durationMinutes = estimatedDurationMinutes.coerceAtLeast(1),
        accent = accentArgb?.let(::Color) ?: original.accent,
        emoji = symbol.ifBlank { original.emoji },
        tags = tags,
        steps = steps.mapIndexed { index, step ->
            val old = originals[step.id]
            val placement = placements[step.id]
            ChecklistStep(
                id = step.id,
                eyebrow = if (step.type == "FINAL") "ГОТОВО" else "%02d · %s".format(index + 1, step.type.replace('_', ' ')),
                title = step.title,
                description = step.description,
                type = when (step.type) {
                    "INFORMATION" -> StepType.Information
                    "WARNING" -> StepType.Warning
                    "YES_NO" -> StepType.YesNo
                    "SINGLE_CHOICE" -> StepType.SingleChoice
                    "MULTIPLE_CHOICE" -> StepType.MultipleChoice
                    "TIMER" -> StepType.Timer
                    "FINAL" -> StepType.Final
                    else -> StepType.Checkbox
                },
                timerSeconds = step.timerSeconds,
                checklistItems = step.checklistItems,
                options = step.options,
                note = old?.note.orEmpty(),
                attachments = old?.attachments.orEmpty(),
                parentConditionId = placement?.first,
                parentOptionIndex = placement?.second,
            )
        },
    )
}
