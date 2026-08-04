package ru.ritual.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

data class AlgorithmUsageEvent(
    val algorithmId: String,
    val startedAtMillis: Long,
    val finishedAtMillis: Long? = null,
)

data class RankedAlgorithmSuggestion(
    val algorithmId: String,
    val reason: String,
    val score: Int,
)

/** Device-local launch history used for recent items and optional contextual suggestions. */
class AlgorithmUsageStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private var events: List<AlgorithmUsageEvent> = read()

    fun recordStart(algorithmId: String, now: Long = System.currentTimeMillis()) {
        val latest = events.lastOrNull()
        if (latest?.algorithmId == algorithmId && latest.finishedAtMillis == null && now - latest.startedAtMillis < DEDUP_WINDOW) return
        events = (events + AlgorithmUsageEvent(algorithmId, now)).takeLast(MAX_EVENTS)
        persist()
    }

    fun recordFinish(algorithmId: String, now: Long = System.currentTimeMillis()) {
        val index = events.indexOfLast { it.algorithmId == algorithmId && it.finishedAtMillis == null }
        if (index < 0) return
        events = events.toMutableList().also { list ->
            list[index] = list[index].copy(finishedAtMillis = now)
        }
        persist()
    }

    fun recentIds(limit: Int = 30): List<String> = events.asReversed()
        .map(AlgorithmUsageEvent::algorithmId)
        .distinct()
        .take(limit)

    fun suggestionsFor(
        algorithmId: String,
        availableIds: Set<String>,
        now: Long = System.currentTimeMillis(),
        limit: Int = 4,
    ): List<RankedAlgorithmSuggestion> {
        if (events.size < 2) return emptyList()
        val zone = ZoneId.systemDefault()
        val currentMoment = Instant.ofEpochMilli(now).atZone(zone)
        val candidates = availableIds - algorithmId
        return candidates.mapNotNull { candidateId ->
            val candidateEvents = events.filter { it.algorithmId == candidateId }
            if (candidateEvents.isEmpty()) return@mapNotNull null

            val transitions = events.zipWithNext().count { (from, to) ->
                from.algorithmId == algorithmId && to.algorithmId == candidateId &&
                    to.startedAtMillis - from.startedAtMillis in 0..TRANSITION_WINDOW
            }
            val sameWeekday = candidateEvents.count {
                Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).dayOfWeek == currentMoment.dayOfWeek
            }
            val closeInTime = candidateEvents.count {
                val eventTime = Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).toLocalTime()
                val delta = abs(eventTime.toSecondOfDay() - currentMoment.toLocalTime().toSecondOfDay())
                minOf(delta, SECONDS_PER_DAY - delta) <= TIME_WINDOW_SECONDS
            }
            val latest = candidateEvents.maxOf(AlgorithmUsageEvent::startedAtMillis)
            val recentBonus = when {
                now - latest <= 24 * HOUR -> 5
                now - latest <= 7 * 24 * HOUR -> 3
                else -> 1
            }
            val score = transitions * 40 + sameWeekday * 5 + closeInTime * 4 + recentBonus
            val reason = when {
                transitions > 0 -> "Обычно запускаете следующим"
                closeInTime > 0 -> "Часто запускаете примерно сейчас"
                sameWeekday > 0 -> "Подходит для этого дня недели"
                else -> "Вы запускали недавно"
            }
            RankedAlgorithmSuggestion(candidateId, reason, score)
        }.sortedWith(compareByDescending<RankedAlgorithmSuggestion> { it.score }.thenBy { it.algorithmId })
            .take(limit)
    }

    private fun persist() {
        val data = JSONArray().apply {
            events.forEach { event ->
                put(
                    JSONObject()
                        .put("algorithmId", event.algorithmId)
                        .put("startedAtMillis", event.startedAtMillis)
                        .put("finishedAtMillis", event.finishedAtMillis ?: JSONObject.NULL),
                )
            }
        }
        preferences.edit().putString(KEY_EVENTS, data.toString()).apply()
    }

    private fun read(): List<AlgorithmUsageEvent> = runCatching {
        val data = JSONArray(preferences.getString(KEY_EVENTS, "[]"))
        buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val id = item.optString("algorithmId").trim()
                if (id.isBlank()) continue
                add(
                    AlgorithmUsageEvent(
                        algorithmId = id,
                        startedAtMillis = item.optLong("startedAtMillis", 0L).takeIf { it > 0L } ?: continue,
                        finishedAtMillis = item.optLong("finishedAtMillis", 0L).takeIf { it > 0L },
                    ),
                )
            }
        }.takeLast(MAX_EVENTS)
    }.getOrDefault(emptyList())

    private companion object {
        const val PREFERENCES = "algorithm_usage"
        const val KEY_EVENTS = "events"
        const val MAX_EVENTS = 300
        const val DEDUP_WINDOW = 5 * 60_000L
        const val HOUR = 3_600_000L
        const val TRANSITION_WINDOW = 8 * HOUR
        const val SECONDS_PER_DAY = 24 * 60 * 60
        const val TIME_WINDOW_SECONDS = 90 * 60
    }
}
