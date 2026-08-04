package ru.ritual.app.data

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import ru.ritual.app.domain.model.ActiveAlgorithmRun
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.progressRange

class ActiveRunStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutableRuns = MutableStateFlow(readAll())
    val activeRuns: StateFlow<List<ActiveAlgorithmRun>> = mutableRuns.asStateFlow()

    fun update(checklist: Checklist, stepIndex: Int, visitedStepIds: List<String>): ActiveAlgorithmRun {
        val previous = mutableRuns.value.firstOrNull { it.algorithmId == checklist.id }
        val safeVisited = visitedStepIds.ifEmpty { listOfNotNull(checklist.steps.getOrNull(stepIndex)?.id) }
        val progress = checklist.progressRange(stepIndex, safeVisited)
        val run = ActiveAlgorithmRun(
            algorithmId = checklist.id,
            title = checklist.title,
            emoji = checklist.emoji,
            accentArgb = checklist.accent.toArgb(),
            currentStepIndex = stepIndex.coerceIn(0, (checklist.steps.size - 1).coerceAtLeast(0)),
            totalSteps = progress.maxTotalSteps,
            completedSteps = progress.completedSteps,
            minTotalSteps = progress.minTotalSteps,
            maxTotalSteps = progress.maxTotalSteps,
            minPercent = progress.minPercent,
            maxPercent = progress.maxPercent,
            visitedStepIds = safeVisited,
            startedAtMillis = previous?.startedAtMillis ?: System.currentTimeMillis(),
        )
        persist(listOf(run) + mutableRuns.value.filterNot { it.algorithmId == checklist.id })
        return run
    }

    fun clear(algorithmId: String) {
        persist(mutableRuns.value.filterNot { it.algorithmId == algorithmId })
    }

    fun clearAll() {
        preferences.edit().clear().apply()
        mutableRuns.value = emptyList()
    }

    private fun persist(runs: List<ActiveAlgorithmRun>) {
        val json = JSONArray().apply { runs.forEach { put(it.toJson()) } }
        preferences.edit().putString(KEY_RUNS, json.toString()).apply()
        mutableRuns.value = runs
    }

    private fun readAll(): List<ActiveAlgorithmRun> {
        val encoded = preferences.getString(KEY_RUNS, null)
        if (!encoded.isNullOrBlank()) {
            return runCatching {
                val array = JSONArray(encoded)
                buildList {
                    for (index in 0 until array.length()) array.optJSONObject(index)?.toRun()?.let(::add)
                }
            }.getOrDefault(emptyList())
        }
        val legacy = readLegacy() ?: return emptyList()
        preferences.edit().putString(
            KEY_RUNS,
            JSONArray().put(legacy.toJson()).toString(),
        ).apply()
        return listOf(legacy)
    }

    private fun ActiveAlgorithmRun.toJson() = JSONObject()
        .put("algorithmId", algorithmId)
        .put("title", title)
        .put("emoji", emoji)
        .put("accentArgb", accentArgb)
        .put("currentStepIndex", currentStepIndex)
        .put("totalSteps", totalSteps)
        .put("completedSteps", completedSteps)
        .put("minTotalSteps", minTotalSteps)
        .put("maxTotalSteps", maxTotalSteps)
        .put("minPercent", minPercent)
        .put("maxPercent", maxPercent)
        .put("visitedStepIds", JSONArray(visitedStepIds))
        .put("startedAtMillis", startedAtMillis)

    private fun JSONObject.toRun(): ActiveAlgorithmRun? {
        val id = optString("algorithmId").takeIf(String::isNotBlank) ?: return null
        val visited = optJSONArray("visitedStepIds")?.let { array ->
            buildList {
                for (index in 0 until array.length()) array.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }.orEmpty()
        return ActiveAlgorithmRun(
            algorithmId = id,
            title = optString("title", "Алгоритм"),
            emoji = optString("emoji", "▶").ifBlank { "▶" },
            accentArgb = optInt("accentArgb", 0xFFDDF56B.toInt()),
            currentStepIndex = optInt("currentStepIndex", 0).coerceAtLeast(0),
            totalSteps = optInt("totalSteps", 1).coerceAtLeast(1),
            completedSteps = optInt("completedSteps", 1).coerceAtLeast(1),
            minTotalSteps = optInt("minTotalSteps", optInt("totalSteps", 1)).coerceAtLeast(1),
            maxTotalSteps = optInt("maxTotalSteps", optInt("totalSteps", 1)).coerceAtLeast(1),
            minPercent = optInt("minPercent", 0).coerceIn(0, 100),
            maxPercent = optInt("maxPercent", 0).coerceIn(0, 100),
            visitedStepIds = visited,
            startedAtMillis = optLong("startedAtMillis", System.currentTimeMillis()),
        )
    }

    private fun readLegacy(): ActiveAlgorithmRun? {
        val id = preferences.getString(KEY_ID, null)?.takeIf(String::isNotBlank) ?: return null
        return ActiveAlgorithmRun(
            algorithmId = id,
            title = preferences.getString(KEY_TITLE, "Алгоритм").orEmpty(),
            emoji = preferences.getString(KEY_EMOJI, "▶").orEmpty().ifBlank { "▶" },
            accentArgb = preferences.getInt(KEY_ACCENT, 0xFFDDF56B.toInt()),
            currentStepIndex = preferences.getInt(KEY_STEP, 0).coerceAtLeast(0),
            totalSteps = preferences.getInt(KEY_TOTAL, 1).coerceAtLeast(1),
            completedSteps = preferences.getInt(KEY_COMPLETED, 1).coerceAtLeast(1),
            minTotalSteps = preferences.getInt(KEY_MIN_TOTAL, preferences.getInt(KEY_TOTAL, 1)).coerceAtLeast(1),
            maxTotalSteps = preferences.getInt(KEY_MAX_TOTAL, preferences.getInt(KEY_TOTAL, 1)).coerceAtLeast(1),
            minPercent = preferences.getInt(KEY_MIN_PERCENT, 0).coerceIn(0, 100),
            maxPercent = preferences.getInt(KEY_MAX_PERCENT, 0).coerceIn(0, 100),
            visitedStepIds = preferences.getString(KEY_VISITED, "").orEmpty().split(SEPARATOR).filter(String::isNotBlank),
            startedAtMillis = preferences.getLong(KEY_STARTED, System.currentTimeMillis()),
        )
    }

    companion object {
        private const val PREFERENCES = "active_algorithm_run"
        private const val KEY_RUNS = "runs_json_v2"
        private const val KEY_ID = "algorithm_id"
        private const val KEY_TITLE = "title"
        private const val KEY_EMOJI = "emoji"
        private const val KEY_ACCENT = "accent"
        private const val KEY_STEP = "step"
        private const val KEY_TOTAL = "total"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_MIN_TOTAL = "min_total"
        private const val KEY_MAX_TOTAL = "max_total"
        private const val KEY_MIN_PERCENT = "min_percent"
        private const val KEY_MAX_PERCENT = "max_percent"
        private const val KEY_VISITED = "visited"
        private const val KEY_STARTED = "started"
        private const val SEPARATOR = "\u001F"
    }
}
